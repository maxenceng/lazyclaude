package dev.maxenceng.lazyclaude.session.domain;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Builds the sessions of a project from the hosted sessions lazyclaude holds and every past one. It owns the rules: a hosted
 * session's state is drawn from the registry record of its own pid, none yet meaning it is starting, and one whose record cannot be
 * used shows as unknown, with one warning naming why, while the others keep their state. Past rows run newest activity first, a past
 * session belongs to the project its {@code cwd} was located in (or, when that directory is gone, to the one its path lies in), and
 * a hosted id hides the past session of the same id. A session is linked to tickets by its transcript, live or past, against the
 * ticket ids it is handed; a hosted session with no transcript yet is linked by the launch that began it.
 */
public final class SessionManager {

    private static final String SWEEP_WARNING = "sessions: %s; checking every %d s instead.";
    private static final String UNREAD_WARNING = "hosted session %s: %s; its state is unknown.";

    private static final String TRANSCRIPT_WARNING = "past sessions: %s; checking every %d s instead.";
    private static final String CACHE_WARNING = "past sessions: %s; they are read in full on every start.";
    private static final String ROOT_WARNING = "past sessions: %s; only those already read show.";

    private static final Comparator<PastSession> MOST_RECENTLY_ACTIVE_FIRST = Comparator.comparing(PastSession::lastActivity)
            .reversed()
            .thenComparing(session -> session.id().value());

    private final LiveSessionPort sessions;
    private final PastSessionPort pastSessions;
    private final ProjectLocatorPort locator;
    private final SessionWatchPort watch;
    private final TranscriptWatchPort transcriptWatch;
    private final WarningPort warnings;
    private final SessionClock clock;
    private final WarnOnce sweeping = new WarnOnce();
    private final WarnOnce pollingTranscripts = new WarnOnce();
    private final WarnOnce cacheUnwritable = new WarnOnce();
    private final WarnOnce rootUnlistable = new WarnOnce();
    private final LastLiveRead lastLive = new LastLiveRead();
    private final LastKnownTickets known = new LastKnownTickets();
    private final UnreadRecords unread = new UnreadRecords();

    public SessionManager(
            LiveSessionPort sessions,
            PastSessionPort pastSessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            Clock clock) {
        this.sessions = sessions;
        this.pastSessions = pastSessions;
        this.locator = locator;
        this.watch = watch;
        this.transcriptWatch = transcriptWatch;
        this.warnings = warnings;
        this.clock = new SessionClock(clock);
    }

    /** Reads the records of the {@code open} hosted sessions again; the past ones come from what the cache already holds. */
    public AvailableSessions sessionsFor(Project project, KnownTickets tickets, HostedSessions open) {
        synchronized (lastLive) {
            return readingLive(project, tickets, open);
        }
    }

    /**
     * Hands {@code listener} the sessions again on every change, among the hosted sessions {@code open} says are open then. A
     * change to a hosted session's record reads them again; a change to the transcripts reuses the last read. Both deliver
     * under one lock, so a slower read never overwrites a newer one.
     */
    public void watch(Project project, KnownTickets tickets, Supplier<HostedSessions> open, Consumer<AvailableSessions> listener) {
        known.know(tickets);
        watch.watch(
                pid -> open.get().pids().contains(pid),
                () -> deliver(listener, () -> readingLive(project, known.current(), open.get())),
                this::warnOfSweep);
        transcriptWatch.watch(
                this::locationOf,
                () -> deliver(listener, () -> reusingLive(project, known.current(), open.get())),
                this::warnOfTranscriptPolling,
                this::warnOfCache,
                this::warnOfUnlistableRoot);
    }

    /** Hands {@code listener} the sessions again now, for a change none of the watchers sees: a hosted session opening or closing. */
    public void refresh(Project project, HostedSessions open, Consumer<AvailableSessions> listener) {
        deliver(listener, () -> readingLive(project, known.current(), open));
    }

    /** From now on sessions are linked against {@code tickets}, and {@code listener} is handed them linked that way at once. */
    public void know(Project project, KnownTickets tickets, HostedSessions open, Consumer<AvailableSessions> listener) {
        known.know(tickets);
        refresh(project, open, listener);
    }

    /** Has the transcripts read again, whether or not they look changed; the sessions arrive through the watch's listener. */
    public void rescanTranscripts() {
        transcriptWatch.rescanNow();
    }

    private void deliver(Consumer<AvailableSessions> listener, Supplier<AvailableSessions> read) {
        synchronized (lastLive) {
            listener.accept(read.get());
        }
    }

    private AvailableSessions readingLive(Project project, KnownTickets tickets, HostedSessions open) {
        RunningSessions running = runningIn(project, open);
        lastLive.remember(running);
        return available(project, tickets, running);
    }

    private AvailableSessions reusingLive(Project project, KnownTickets tickets, HostedSessions open) {
        return lastLive.of(project)
                .map(running -> available(project, tickets, running))
                .orElseGet(() -> readingLive(project, tickets, open));
    }

    private AvailableSessions available(Project project, KnownTickets tickets, RunningSessions running) {
        Map<SessionId, LinkFacts> facts = pastSessions.linkFacts();
        return AvailableSessions.builder()
                .hosted(running.hosted().stream()
                        .map(hosted -> hosted.linkedTo(linksOf(hosted.session().id(), facts, tickets)))
                        .toList())
                .past(pastSessionsOf(project, running.ids()).stream()
                        .map(session -> session.linkedTo(linksOf(session.id(), facts, tickets)))
                        .toList())
                .clock(clock.frozen())
                .build();
    }

    private static List<TicketLink> linksOf(SessionId id, Map<SessionId, LinkFacts> facts, KnownTickets tickets) {
        return facts.getOrDefault(id, LinkFacts.none()).linksTo(tickets);
    }

    private RunningSessions runningIn(Project project, HostedSessions open) {
        return RunningSessions.builder()
                .project(project)
                .hosted(open.sessions().stream().map(this::opened).toList())
                .build();
    }

    private OpenHostedSession opened(HostedSession session) {
        return OpenHostedSession.builder().session(session).phase(phaseOf(session)).build();
    }

    private HostedPhase phaseOf(HostedSession session) {
        try {
            Optional<ReportedSession> record = sessions.recordOf(session.pid());
            unread.readable(session.id());
            return record.<HostedPhase>map(reported -> new LivePhase(live(session, reported))).orElseGet(StartingPhase::new);
        } catch (SessionRecordUnreadableException unusable) {
            if (unread.firstUnreadOf(session.id())) {
                warnings.raise(new Warning(UNREAD_WARNING.formatted(session.id().shortValue(), unusable.getMessage())));
            }
            return new UnreadPhase(unknown(session));
        }
    }

    private void warnOfTranscriptPolling(Reason cause, Duration every) {
        pollingTranscripts.raise(warnings, new Warning(TRANSCRIPT_WARNING.formatted(cause.value(), every.toSeconds())));
    }

    private void warnOfCache(Reason cause) {
        cacheUnwritable.raise(warnings, new Warning(CACHE_WARNING.formatted(cause.value())));
    }

    private void warnOfUnlistableRoot(Reason cause) {
        rootUnlistable.raise(warnings, new Warning(ROOT_WARNING.formatted(cause.value())));
    }

    private ProjectLocation locationOf(WorkingDirectory directory) {
        try {
            return new LocatedInProject(locator.locate(directory));
        } catch (ProjectNotFoundException outsideAnyProject) {
            return new NotLocated();
        }
    }

    private void warnOfSweep(Reason cause, Duration every) {
        sweeping.raise(warnings, new Warning(SWEEP_WARNING.formatted(cause.value(), every.toSeconds())));
    }

    private List<PastSession> pastSessionsOf(Project project, List<SessionId> running) {
        return pastSessions.pastSessions().stream()
                .filter(located -> !running.contains(located.session().id()))
                .filter(located -> located.belongsTo(project))
                .map(LocatedPastSession::session)
                .sorted(MOST_RECENTLY_ACTIVE_FIRST)
                .toList();
    }

    private static LiveSession live(HostedSession session, ReportedSession reported) {
        return LiveSession.builder()
                .id(session.id())
                .pid(session.pid())
                .name(reported.name())
                .directory(session.directory())
                .startedAt(reported.startedAt())
                .version(reported.version())
                .state(reported.state())
                .build();
    }

    // deferred: an unreadable record leaves the session's start time unknown, so its age counts from this read — close it when the launch log keeps when each session started.
    private LiveSession unknown(HostedSession session) {
        return LiveSession.builder()
                .id(session.id())
                .pid(session.pid())
                .name(session.name())
                .directory(session.directory())
                .startedAt(new StartedAt(clock.clock().instant()))
                .version(new NoVersion())
                .state(new Unknown())
                .build();
    }
}

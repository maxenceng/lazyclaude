package dev.maxenceng.lazyclaude.session.domain;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Builds the sessions of a project from the hosted sessions lazyclaude holds and its launch log. It owns the rules: a hosted
 * session's state and the session it runs now are drawn from the registry record of its own pid, none yet (or one left by an earlier
 * process of that pid) meaning it is starting, and one whose record cannot be
 * used shows as unknown, with one warning naming why, while the others keep their state. A past session is a launch-log entry with
 * no hosted session of its id, and only logged ids' transcripts are asked for. Past rows list the interrupted ones first, then
 * newest activity first. An entry whose transcript is gone, with no live process, is dropped from the log. A session is linked
 * to tickets by the launch that began it, then by the {@code /ticket} runs its transcript records, against the ticket ids it is handed.
 */
public final class SessionManager {

    private static final String SWEEP_WARNING = "sessions: %s; checking every %d s instead.";
    private static final String UNREAD_WARNING = "hosted session %s: %s; its state is unknown.";

    private static final String TRANSCRIPT_WARNING = "past sessions: %s; checking every %d s instead.";
    private static final String CACHE_WARNING = "past sessions: %s; they are read in full on every start.";
    private static final String ROOT_WARNING = "past sessions: %s; only those already read show.";

    private static final Comparator<PastSession> INTERRUPTED_THEN_MOST_RECENTLY_ACTIVE_FIRST = Comparator.comparing(
                    PastSession::interrupted, Comparator.reverseOrder())
            .thenComparing(PastSession::lastActivity, Comparator.reverseOrder())
            .thenComparing(session -> session.id().value());

    private final LiveSessionPort sessions;
    private final PastSessionPort pastSessions;
    private final LaunchLogPort log;
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
            LaunchLogPort log,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            Clock clock) {
        this.sessions = sessions;
        this.pastSessions = pastSessions;
        this.log = log;
        this.watch = watch;
        this.transcriptWatch = transcriptWatch;
        this.warnings = warnings;
        this.clock = new SessionClock(clock);
    }

    /** Reads the records of the {@code open} hosted sessions again; the past ones come from what the cache already holds. */
    public AvailableSessions sessionsFor(Project project, KnownTickets tickets, HostedSessions open) {
        synchronized (lastLive) {
            return readingLive(project, tickets, () -> open);
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
                () -> deliver(listener, () -> readingLive(project, known.current(), open)),
                this::warnOfSweep);
        transcriptWatch.watch(
                () -> logOf(project).ids(),
                () -> deliver(listener, () -> reusingLive(project, known.current(), open)),
                this::warnOfTranscriptPolling,
                this::warnOfCache,
                this::warnOfUnlistableRoot);
    }

    /** Hands {@code listener} the sessions again now, for a change none of the watchers sees: a hosted session opening or closing. */
    public void refresh(Project project, HostedSessions open, Consumer<AvailableSessions> listener) {
        deliver(listener, () -> readingLive(project, known.current(), () -> open));
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

    private AvailableSessions readingLive(Project project, KnownTickets tickets, Supplier<HostedSessions> open) {
        RunningSessions running = runningIn(project, open.get());
        lastLive.remember(running);
        return available(project, tickets, running, open);
    }

    private AvailableSessions reusingLive(Project project, KnownTickets tickets, Supplier<HostedSessions> open) {
        return lastLive.of(project)
                .map(running -> available(project, tickets, running, open))
                .orElseGet(() -> readingLive(project, tickets, open));
    }

    private AvailableSessions available(
            Project project, KnownTickets tickets, RunningSessions running, Supplier<HostedSessions> open) {
        LaunchLog logged = logOf(project);
        LoggedTranscripts transcripts = pastSessions.transcriptsOf(logged.ids());
        Set<SessionId> dropped = droppedFrom(project, running, open.get(), transcripts);
        return AvailableSessions.builder()
                .hosted(running.hosted().stream()
                        .map(hosted -> hosted.linkedTo(linksOfHosted(hosted, logged, transcripts, tickets)))
                        .toList())
                .past(logged.entries().stream()
                        .filter(entry -> !running.ids().contains(entry.id()) && !dropped.contains(entry.id()))
                        .flatMap(entry -> pastSessionOf(entry, transcripts, tickets).stream())
                        .sorted(INTERRUPTED_THEN_MOST_RECENTLY_ACTIVE_FIRST)
                        .toList())
                .clock(clock.frozen())
                .build();
    }

    /**
     * The logged sessions with no live process whose transcript is gone, which are dropped from the log: {@code claude --resume}
     * could not open them. {@code openAfterLog} is read after the log, and a launch is open before it is logged, so a session
     * logged while its transcript is not yet written is never dropped, however old {@code running} is.
     */
    private Set<SessionId> droppedFrom(
            Project project, RunningSessions running, HostedSessions openAfterLog, LoggedTranscripts transcripts) {
        Set<SessionId> justOpened = openAfterLog.sessions().stream().map(HostedSession::id).collect(Collectors.toUnmodifiableSet());
        Set<SessionId> dropped = transcripts.gone().stream()
                .filter(id -> !running.ids().contains(id) && !justOpened.contains(id))
                .collect(Collectors.toUnmodifiableSet());
        if (!dropped.isEmpty()) {
            changeLog(project, logged -> logged.without(dropped));
        }
        return dropped;
    }

    private Optional<PastSession> pastSessionOf(LaunchEntry entry, LoggedTranscripts transcripts, KnownTickets tickets) {
        return transcripts.of(entry.id())
                .map(facts -> PastSession.builder()
                        .id(entry.id())
                        .title(facts.title())
                        .opening(facts.opening())
                        .reading(facts.reading())
                        .directory(entry.directory())
                        .lastActivity(facts.lastActivity())
                        .startedAt(entry.startedAt())
                        .links(facts.links().launchedAs(entry.command().launchRun()).linksTo(tickets))
                        .interrupted(entry.hasNoEnd())
                        .build());
    }

    private static List<TicketLink> linksOfHosted(
            OpenHostedSession hosted, LaunchLog logged, LoggedTranscripts transcripts, KnownTickets tickets) {
        LaunchCommand launch = logged.entryOf(hosted.session().id()).map(LaunchEntry::command).orElse(hosted.session().command());
        return transcripts.of(hosted.id())
                .map(TranscriptFacts::links)
                .orElseGet(() -> LinkFacts.builder().build())
                .launchedAs(launch.launchRun())
                .linksTo(tickets);
    }

    private LaunchLog logOf(Project project) {
        return log.read(project, this::warnOfLog);
    }

    private void changeLog(Project project, UnaryOperator<LaunchLog> change) {
        log.change(project, this::warnOfLog, change);
    }

    private void warnOfLog(Reason cause) {
        warnings.raise(LaunchLogWarning.of(cause));
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
            Optional<ReportedSession> record = sessions.recordOf(session.pid()).filter(session::reportedBy);
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

    private void warnOfSweep(Reason cause, Duration every) {
        sweeping.raise(warnings, new Warning(SWEEP_WARNING.formatted(cause.value(), every.toSeconds())));
    }

    private static LiveSession live(HostedSession session, ReportedSession reported) {
        return LiveSession.builder()
                .id(reported.session())
                .pid(session.pid())
                .name(reported.name())
                .directory(session.directory())
                .startedAt(reported.startedAt())
                .version(reported.version())
                .state(reported.state())
                .build();
    }

    private static LiveSession unknown(HostedSession session) {
        return LiveSession.builder()
                .id(session.id())
                .pid(session.pid())
                .name(session.name())
                .directory(session.directory())
                .startedAt(session.spawnedAt())
                .version(new NoVersion())
                .state(new Unknown())
                .build();
    }
}

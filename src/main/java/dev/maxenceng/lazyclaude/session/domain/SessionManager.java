package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.ProjectUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Builds the sessions of a project from every live session on the machine and every past one. It owns the rules: an
 * {@code sdk-cli} session is not listed, a session belongs to the project its {@code cwd} is located
 * in (or, when git cannot place it, the one its path lies in), rows run newest first, and a session's
 * state is drawn from its reported status only once it is known to be the project's, so a status no
 * one here knows, on a session elsewhere, changes nothing. Each fallback is warned about once, however
 * often its source reports it. A live id hides the past session of the same id, a past session belongs to
 * the project its {@code cwd} was located in (or, when that directory is gone, to the one its path lies in), and
 * past rows run newest activity first. A session is linked to tickets by its transcript, live or past, against the ticket ids it is
 * handed; a live session with no transcript yet is linked to none. A live session is hosted when its pid is one lazyclaude
 * spawned, and each open hosted session is listed as a tab, starting until the registry lists its pid.
 */
public final class SessionManager {

    private static final String SWEEP_WARNING = "sessions: %s; checking every %d s instead.";
    private static final String AGENTS_WARNING = "sessions: %s; asking claude agents --json every %d s instead.";

    private static final String TRANSCRIPT_WARNING = "past sessions: %s; checking every %d s instead.";
    private static final String CACHE_WARNING = "past sessions: %s; they are read in full on every start.";
    private static final String ROOT_WARNING = "past sessions: %s; only those already read show.";

    private static final Comparator<PastSession> MOST_RECENTLY_ACTIVE_FIRST = Comparator.comparing(PastSession::lastActivity)
            .reversed()
            .thenComparing(session -> session.id().value());

    private static final Comparator<ReportedSession> NEWEST_FIRST = Comparator.comparing(ReportedSession::startedAt)
            .reversed()
            .thenComparing(session -> session.id().value());

    private final LiveSessionPort sessions;
    private final PastSessionPort pastSessions;
    private final ProjectLocatorPort locator;
    private final SessionWatchPort watch;
    private final TranscriptWatchPort transcriptWatch;
    private final WarningPort warnings;
    private final SessionClock clock;
    private final HostedSessionsSource hosted;
    private final WarnOnce sweeping = new WarnOnce();
    private final WarnOnce askingAgents = new WarnOnce();
    private final WarnOnce pollingTranscripts = new WarnOnce();
    private final WarnOnce cacheUnwritable = new WarnOnce();
    private final WarnOnce rootUnlistable = new WarnOnce();
    private final LastLiveRead lastLive = new LastLiveRead();

    public SessionManager(
            LiveSessionPort sessions,
            PastSessionPort pastSessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            SessionClock clock,
            HostedSessionsSource hosted) {
        this.sessions = sessions;
        this.pastSessions = pastSessions;
        this.locator = locator;
        this.watch = watch;
        this.transcriptWatch = transcriptWatch;
        this.warnings = warnings;
        this.clock = clock;
        this.hosted = hosted;
    }

    /** Reads the live sessions again; the past ones come from what the cache already holds. */
    public Sessions sessionsFor(Project project, KnownTickets tickets) {
        synchronized (lastLive) {
            return readingLive(project, tickets);
        }
    }

    /**
     * Hands {@code listener} the sessions again on every change. A change to the registry reads the live sessions again; a
     * change to the transcripts reuses the last read of them. Both deliver under one lock, so a slower read never
     * overwrites a newer one.
     */
    public void watch(Project project, KnownTickets tickets, Consumer<Sessions> listener) {
        watch.watch(() -> deliver(listener, () -> readingLive(project, tickets)), this::warnOfSweep);
        transcriptWatch.watch(
                this::locationOf,
                () -> deliver(listener, () -> reusingLive(project, tickets)),
                this::warnOfTranscriptPolling,
                this::warnOfCache,
                this::warnOfUnlistableRoot);
    }

    /** Hands {@code listener} the sessions again now, as a change to which sessions are hosted is not one the watchers see. */
    public void refresh(Project project, KnownTickets tickets, Consumer<Sessions> listener) {
        deliver(listener, () -> readingLive(project, tickets));
    }

    private void deliver(Consumer<Sessions> listener, Supplier<Sessions> read) {
        synchronized (lastLive) {
            listener.accept(read.get());
        }
    }

    private Sessions readingLive(Project project, KnownTickets tickets) {
        try {
            RunningSessions running = runningIn(project);
            lastLive.remember(running);
            return available(project, tickets, running);
        } catch (SessionsUnavailableException noSource) {
            lastLive.forget();
            return new UnavailableSessions(new Reason(noSource.getMessage()));
        } catch (ProjectUnavailableException noLocator) {
            lastLive.forget();
            return new UnavailableSessions(new Reason(noLocator.getMessage()));
        }
    }

    private Sessions reusingLive(Project project, KnownTickets tickets) {
        return lastLive.of(project)
                .map(running -> available(project, tickets, running))
                .orElseGet(() -> readingLive(project, tickets));
    }

    private Sessions available(Project project, KnownTickets tickets, RunningSessions running) {
        Map<SessionId, LinkFacts> facts = pastSessions.linkFacts();
        HostedSessions open = hosted.open();
        List<LiveSession> live = running.live().stream()
                .map(session -> session.linkedTo(linksOf(session.id(), facts, tickets)).ownedAmong(open.pids()))
                .toList();
        return AvailableSessions.builder()
                .live(live)
                .hosted(open.sessions().stream()
                        .map(session -> HostedTab.builder()
                                .session(session)
                                .phase(session.phaseAmong(live))
                                .build())
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

    private RunningSessions runningIn(Project project) {
        List<ReportedSession> reported = sessions.liveSessions(this::warnOfAgents);
        return RunningSessions.builder()
                .project(project)
                .ids(reported.stream().map(ReportedSession::id).toList())
                .live(liveSessionsOf(project, reported))
                .build();
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

    private void warnOfAgents(Reason cause, Duration every) {
        askingAgents.raise(warnings, new Warning(AGENTS_WARNING.formatted(cause.value(), every.toSeconds())));
    }

    private List<PastSession> pastSessionsOf(Project project, List<SessionId> running) {
        return pastSessions.pastSessions().stream()
                .filter(located -> !running.contains(located.session().id()))
                .filter(located -> located.belongsTo(project))
                .map(LocatedPastSession::session)
                .sorted(MOST_RECENTLY_ACTIVE_FIRST)
                .toList();
    }

    private List<LiveSession> liveSessionsOf(Project project, List<ReportedSession> reported) {
        List<ReportedSession> candidates = reported.stream()
                .filter(session -> !session.isSdkCli())
                .toList();
        // deferred: git runs once per distinct cwd on every read of the registry, about every 2 s — close it when that shows up as load on a project with many worktrees.
        Map<WorkingDirectory, ProjectLocation> located = candidates.stream()
                .map(ReportedSession::directory)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::locationOf));
        return candidates.stream()
                .filter(session -> located.get(session.directory()).holds(project, session.directory()))
                .sorted(NEWEST_FIRST)
                .map(SessionManager::live)
                .toList();
    }

    private static LiveSession live(ReportedSession session) {
        return LiveSession.builder()
                .id(session.id())
                .pid(session.pid())
                .name(session.name())
                .directory(session.directory())
                .startedAt(session.startedAt())
                .version(session.version())
                .state(stateOf(session.status(), session.waitingFor()))
                .build();
    }

    /** A waiting session blocked on a permission prompt asks for permission; any other reason asks for input. */
    private static SessionState stateOf(ReportedStatus status, WaitingFor waitingFor) {
        return switch (status) {
            case BUSY, SHELL -> new Busy();
            case IDLE -> new YourTurn();
            case WAITING -> waiting(waitingFor);
            case UNKNOWN -> new Unknown();
        };
    }

    private static SessionState waiting(WaitingFor waitingFor) {
        return switch (waitingFor) {
            case WaitingReason reason when reason.isPermissionPrompt() -> new WaitingForPermission();
            default -> new WaitingForInput(waitingFor);
        };
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionState;
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
 * Builds the sessions of a project from every live session on the machine. It owns the rules: an
 * {@code sdk-cli} session is not listed, a session belongs to the project its {@code cwd} is located
 * in (and one whose {@code cwd} belongs to none is nobody's), rows run newest first, and a session's
 * state is drawn from its reported status only once it is known to be the project's, so a status no
 * one here knows, on a session elsewhere, changes nothing. Each fallback is warned about once, however
 * often its source reports it.
 */
public final class SessionManager {

    private static final String SWEEP_WARNING = "sessions: %s; checking every %d s instead.";
    private static final String AGENTS_WARNING = "sessions: %s; asking claude agents --json every %d s instead.";

    private static final Comparator<ReportedSession> NEWEST_FIRST = Comparator.comparing(ReportedSession::startedAt)
            .reversed()
            .thenComparing(session -> session.id().value());

    private final LiveSessionPort sessions;
    private final ProjectLocatorPort locator;
    private final SessionWatchPort watch;
    private final WarningPort warnings;
    private final SessionClock clock;
    private final WarnOnce sweeping = new WarnOnce();
    private final WarnOnce askingAgents = new WarnOnce();

    public SessionManager(
            LiveSessionPort sessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            WarningPort warnings,
            SessionClock clock) {
        this.sessions = sessions;
        this.locator = locator;
        this.watch = watch;
        this.warnings = warnings;
        this.clock = clock;
    }

    public Sessions sessionsFor(Project project) {
        try {
            return LiveSessions.builder()
                    .sessions(liveSessionsOf(project))
                    .clock(clock.frozen())
                    .build();
        } catch (SessionsUnavailableException noSource) {
            return new UnavailableSessions(new Reason(noSource.getMessage()));
        } catch (ProjectUnavailableException noLocator) {
            return new UnavailableSessions(new Reason(noLocator.getMessage()));
        }
    }

    public void watch(Runnable changed) {
        watch.watch(changed, this::warnOfSweep);
    }

    private void warnOfSweep(Reason cause, Duration every) {
        sweeping.raise(warnings, new Warning(SWEEP_WARNING.formatted(cause.value(), every.toSeconds())));
    }

    private void warnOfAgents(Reason cause, Duration every) {
        askingAgents.raise(warnings, new Warning(AGENTS_WARNING.formatted(cause.value(), every.toSeconds())));
    }

    private List<LiveSession> liveSessionsOf(Project project) {
        List<ReportedSession> candidates = sessions.liveSessions(this::warnOfAgents).stream()
                .filter(session -> !session.isSdkCli())
                .toList();
        // deferred: git runs once per distinct cwd on every refresh, about every 2 s — close it when that shows up as load on a project with many worktrees.
        Map<WorkingDirectory, Optional<Project>> located = candidates.stream()
                .map(ReportedSession::directory)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::locate));
        return candidates.stream()
                .filter(session -> located.get(session.directory()).filter(project::equals).isPresent())
                .sorted(NEWEST_FIRST)
                .map(SessionManager::live)
                .toList();
    }

    private Optional<Project> locate(WorkingDirectory directory) {
        try {
            return Optional.of(locator.locate(directory));
        } catch (ProjectNotFoundException outsideAnyProject) {
            return Optional.empty();
        }
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

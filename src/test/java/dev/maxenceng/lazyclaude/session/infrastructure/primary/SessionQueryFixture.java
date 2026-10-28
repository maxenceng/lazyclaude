package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.hostingATicketSessionInTheWorktree;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.hostingTheRegisteredSession;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLogPort;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.quietPastSessions;
import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LaunchLogPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class SessionQueryFixture {

    private SessionQueryFixture() {}

    public static SessionQuery sessionQuery(LiveSessionPort sessions, LaunchLogPort log, SessionWatchPort watch, WarningPort warnings) {
        return sessionQuery(sessions, quietPastSessions(), log, watch, mock(TranscriptWatchPort.class), warnings);
    }

    public static SessionQuery sessionQuery(
            LiveSessionPort sessions,
            PastSessionPort past,
            LaunchLogPort log,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings) {
        return new SessionQuery(sessionApplicationService(sessions, past, log, watch, transcriptWatch, warnings, mock(HostPort.class)));
    }

    public static SessionQuery sessionQueryHosting(HostPort hosts) {
        return new SessionQuery(sessionApplicationService(
                mock(LiveSessionPort.class),
                quietPastSessions(),
                emptyLaunchLogPort(),
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                mock(WarningPort.class),
                hosts));
    }

    public static HostedSessionCommand hostedSessionCommand(HostPort hosts, WarningPort warnings) {
        return hostedSessionCommand(hosts, warnings, mock(WorktreePort.class));
    }

    public static HostedSessionCommand hostedSessionCommand(HostPort hosts, WarningPort warnings, WorktreePort worktrees) {
        return new HostedSessionCommand(sessionApplicationService(
                mock(LiveSessionPort.class),
                quietPastSessions(),
                emptyLaunchLogPort(),
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                warnings,
                hosts,
                worktrees));
    }

    public static SessionQuery sessionQueryOver(LiveSessionPort sessions, LaunchLogPort log, WorktreePort worktrees) {
        return new SessionQuery(worktreeService(sessions, log, worktrees));
    }

    public static TicketWorktreeCommand ticketWorktreeCommand(
            LiveSessionPort sessions, LaunchLogPort log, WorktreePort worktrees) {
        return new TicketWorktreeCommand(worktreeService(sessions, log, worktrees));
    }

    public static SessionQuery sessionQueryHostingTheRegistered(
            LiveSessionPort sessions,
            PastSessionPort past,
            LaunchLogPort log,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings) {
        HostPort hosts = mock(HostPort.class);
        return new SessionQuery(hostingTheRegisteredSession(
                sessionApplicationService(sessions, past, log, watch, transcriptWatch, warnings, hosts), hosts));
    }

    public static SessionQuery sessionQueryOverOccupied(LiveSessionPort sessions, WorktreePort worktrees) {
        return new SessionQuery(occupiedWorktreeService(sessions, worktrees));
    }

    public static TicketWorktreeCommand ticketWorktreeCommandOverOccupied(LiveSessionPort sessions, WorktreePort worktrees) {
        return new TicketWorktreeCommand(occupiedWorktreeService(sessions, worktrees));
    }

    private static SessionApplicationService occupiedWorktreeService(LiveSessionPort sessions, WorktreePort worktrees) {
        HostPort hosts = mock(HostPort.class);
        return hostingATicketSessionInTheWorktree(
                sessionApplicationService(
                        sessions,
                        quietPastSessions(),
                        emptyLaunchLogPort(),
                        mock(SessionWatchPort.class),
                        mock(TranscriptWatchPort.class),
                        mock(WarningPort.class),
                        hosts,
                        worktrees),
                hosts,
                worktrees);
    }

    private static SessionApplicationService worktreeService(
            LiveSessionPort sessions, LaunchLogPort log, WorktreePort worktrees) {
        HostPort hosts = mock(HostPort.class);
        return hostingTheRegisteredSession(
                sessionApplicationService(
                        sessions,
                        quietPastSessions(),
                        log,
                        mock(SessionWatchPort.class),
                        mock(TranscriptWatchPort.class),
                        mock(WarningPort.class),
                        hosts,
                        worktrees),
                hosts);
    }
}

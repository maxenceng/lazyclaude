package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.hostingTheRegisteredSession;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class SessionQueryFixture {

    private SessionQueryFixture() {}

    public static SessionQuery sessionQuery(LiveSessionPort sessions, ProjectLocatorPort locator, SessionWatchPort watch, WarningPort warnings) {
        return sessionQuery(sessions, mock(PastSessionPort.class), locator, watch, mock(TranscriptWatchPort.class), warnings);
    }

    public static SessionQuery sessionQuery(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings) {
        return new SessionQuery(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, mock(HostPort.class)));
    }

    public static SessionQuery sessionQueryHosting(HostPort hosts) {
        return new SessionQuery(sessionApplicationService(
                mock(LiveSessionPort.class),
                mock(PastSessionPort.class),
                mock(ProjectLocatorPort.class),
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
                mock(PastSessionPort.class),
                mock(ProjectLocatorPort.class),
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                warnings,
                hosts,
                worktrees));
    }

    public static SessionQuery sessionQueryOver(LiveSessionPort sessions, ProjectLocatorPort locator, WorktreePort worktrees) {
        return new SessionQuery(worktreeService(sessions, locator, worktrees));
    }

    public static TicketWorktreeCommand ticketWorktreeCommand(
            LiveSessionPort sessions, ProjectLocatorPort locator, WorktreePort worktrees) {
        return new TicketWorktreeCommand(worktreeService(sessions, locator, worktrees));
    }

    public static SessionQuery sessionQueryHostingTheRegistered(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings) {
        HostPort hosts = mock(HostPort.class);
        return new SessionQuery(hostingTheRegisteredSession(
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts), hosts));
    }

    private static SessionApplicationService worktreeService(
            LiveSessionPort sessions, ProjectLocatorPort locator, WorktreePort worktrees) {
        HostPort hosts = mock(HostPort.class);
        return hostingTheRegisteredSession(
                sessionApplicationService(
                        sessions,
                        mock(PastSessionPort.class),
                        locator,
                        mock(SessionWatchPort.class),
                        mock(TranscriptWatchPort.class),
                        mock(WarningPort.class),
                        hosts,
                        worktrees),
                hosts);
    }
}

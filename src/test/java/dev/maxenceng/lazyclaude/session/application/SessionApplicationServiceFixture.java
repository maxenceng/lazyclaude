package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketStart;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class SessionApplicationServiceFixture {

    private SessionApplicationServiceFixture() {}

    public static SessionApplicationService sessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts) {
        return sessionApplicationService(
                sessions, past, locator, watch, transcriptWatch, warnings, hosts, mock(WorktreePort.class));
    }

    public static SessionApplicationService sessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts,
            WorktreePort worktrees) {
        return sessionApplicationService(
                sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees, mock(DesktopNotificationPort.class), true);
    }

    public static SessionApplicationService sessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            DesktopNotificationPort notifications) {
        return sessionApplicationService(
                sessions, past, locator, watch, transcriptWatch, warnings, mock(HostPort.class), mock(WorktreePort.class), notifications, true);
    }

    public static SessionApplicationService sessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts,
            DesktopNotificationPort notifications) {
        return sessionApplicationService(
                sessions, past, locator, watch, transcriptWatch, warnings, hosts, mock(WorktreePort.class), notifications, true);
    }

    public static SessionApplicationService sessionApplicationServiceWithoutDesktopNotifications(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts,
            DesktopNotificationPort notifications) {
        return sessionApplicationService(
                sessions, past, locator, watch, transcriptWatch, warnings, hosts, mock(WorktreePort.class), notifications, false);
    }

    public static SessionApplicationService hostingTheRegisteredSession(SessionApplicationService service, HostPort hosts) {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        service.startUnlinked(project(), paneSize());
        return service;
    }

    public static SessionId startedUnlinked(SessionApplicationService service, HostPort hosts) {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        return ((StartedHost) service.startUnlinked(project(), paneSize())).id();
    }

    public static SessionApplicationService hostingATicketSessionInTheWorktree(
            SessionApplicationService service, HostPort hosts, WorktreePort worktrees) {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());
        service.startTicket(ticketStart(Verb.DESIGN), paneSize());
        return service;
    }

    private static SessionApplicationService sessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts,
            WorktreePort worktrees,
            DesktopNotificationPort notifications,
            boolean desktopNotifications) {
        return new SessionApplicationService(
                sessions,
                past,
                locator,
                watch,
                transcriptWatch,
                warnings,
                hosts,
                worktrees,
                notifications,
                desktopNotifications,
                clock());
    }
}

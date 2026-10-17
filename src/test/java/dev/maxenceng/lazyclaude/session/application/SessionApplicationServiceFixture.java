package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
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
                sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees, mock(DesktopNotificationPort.class));
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
                sessions, past, locator, watch, transcriptWatch, warnings, mock(HostPort.class), mock(WorktreePort.class), notifications);
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
            DesktopNotificationPort notifications) {
        return new SessionApplicationService(
                sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees, notifications, clock());
    }
}

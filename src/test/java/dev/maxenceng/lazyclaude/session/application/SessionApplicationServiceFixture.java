package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.kittyConfigFile;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inKitty;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.emptyEnvironmentPort;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.workingDirectoryOf;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigPort;
import dev.maxenceng.lazyclaude.session.domain.KittySettings;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentPort;
import dev.maxenceng.lazyclaude.session.domain.RemoteControlOffException;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TerminalJumpPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptReadPort;
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

    public static SessionApplicationService sessionApplicationServiceWithoutDesktopNotifications(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            DesktopNotificationPort notifications) {
        return sessionApplicationService(
                sessions, past, locator, watch, transcriptWatch, warnings, mock(HostPort.class), mock(WorktreePort.class), notifications, false);
    }

    public static SessionApplicationService outsideSessionApplicationService(
            LiveSessionPort sessions,
            ProjectLocatorPort locator,
            WarningPort warnings,
            HostPort hosts,
            ProcessEnvironmentPort environments,
            TerminalJumpPort jumps,
            KittyConfigPort kittyConfig) {
        return new SessionApplicationService(
                sessions,
                mock(PastSessionPort.class),
                locator,
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                warnings,
                hosts,
                mock(WorktreePort.class),
                mock(DesktopNotificationPort.class),
                environments,
                jumps,
                kittyConfig,
                mock(TranscriptReadPort.class),
                true,
                clock());
    }

    public static void anOutsideSessionInKitty(
            LiveSessionPort sessions, ProjectLocatorPort locator, ProcessEnvironmentPort environments) {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(environments.of(processId())).thenReturn(kittyEnvironment());
        when(environments.ownEnvironment()).thenReturn(kittyEnvironment());
    }

    public static void kittyRefusingRemoteControl(TerminalJumpPort jumps, KittyConfigPort kittyConfig, KittySettings settings) {
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(kittyConfig.locate()).thenReturn(kittyConfigFile());
        when(kittyConfig.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(settings);
    }

    public static SessionApplicationService transcriptReadingService(TranscriptReadPort transcripts) {
        return new SessionApplicationService(
                mock(LiveSessionPort.class),
                mock(PastSessionPort.class),
                mock(ProjectLocatorPort.class),
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                mock(WarningPort.class),
                mock(HostPort.class),
                mock(WorktreePort.class),
                mock(DesktopNotificationPort.class),
                emptyEnvironmentPort(),
                mock(TerminalJumpPort.class),
                mock(KittyConfigPort.class),
                transcripts,
                true,
                clock());
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
                emptyEnvironmentPort(),
                mock(TerminalJumpPort.class),
                mock(KittyConfigPort.class),
                mock(TranscriptReadPort.class),
                desktopNotifications,
                clock());
    }
}

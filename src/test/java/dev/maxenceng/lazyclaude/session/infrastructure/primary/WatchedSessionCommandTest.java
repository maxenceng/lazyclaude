package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.hostingTheRegisteredSession;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.inputNotification;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.notificationId;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWaitingFor;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWith;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class WatchedSessionCommandTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private PastSessionPort past;

    @Mock
    private TranscriptWatchPort transcriptWatch;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Mock
    private DesktopNotificationPort notifications;

    @Mock
    private HostPort hosts;

    @Test
    void shouldCloseTheNotificationOfTheSessionItIsToldIsInView() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.BUSY)))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        SessionApplicationService service =
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications);
        hostingTheRegisteredSession(service, hosts);
        SessionId id = sessionId();
        service.subscribe(project(), noTickets(), read -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());
        changed.getValue().run();
        changed.getValue().run();

        new WatchedSessionCommand(service).watching(new WatchedSessionPayload(id));

        verify(notifications).close(notificationId());
    }

    @Test
    void shouldNotifyAgainOnlyOnTheNextTransitionOnceNothingIsInView() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.BUSY)))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        SessionApplicationService service =
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications);
        hostingTheRegisteredSession(service, hosts);
        SessionId id = sessionId();
        service.subscribe(project(), noTickets(), read -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());
        WatchedSessionCommand command = new WatchedSessionCommand(service);
        changed.getValue().run();
        command.watching(new WatchedSessionPayload(id));
        changed.getValue().run();

        command.watching(new NoWatchedSessionPayload());
        changed.getValue().run();

        verify(notifications, never()).show(any());
    }
}

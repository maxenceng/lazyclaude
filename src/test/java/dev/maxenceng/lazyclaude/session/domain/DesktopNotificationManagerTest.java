package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.inputNotification;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.managerHavingSeen;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.notificationId;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.otherProjectNotification;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.permissionNotification;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.secondNotificationId;
import static dev.maxenceng.lazyclaude.session.domain.ElsewhereGroupFixture.otherProjectGroup;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.elsewhereOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class DesktopNotificationManagerTest {

    @Mock
    private DesktopNotificationPort notifications;

    @Mock
    private WarningPort warnings;

    @Test
    void shouldFireNothingOnTheFirstReadEvenForASessionAlreadyWaiting() {
        new DesktopNotificationManager(notifications, warnings)
                .seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verifyNoInteractions(notifications);
    }

    @Test
    void shouldFireWhenASessionEntersWaitingAfterTheFirstRead() {
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = new DesktopNotificationManager(notifications, warnings);
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Busy()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verify(notifications).show(inputNotification());
    }

    @Test
    void shouldCarryThePermissionPromptsReasonInTheBody() {
        when(notifications.show(permissionNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForPermission()))));

        verify(notifications).show(permissionNotification());
    }

    @Test
    void shouldFireNothingForYourTurnBusyOrUnknown() {
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(waitingForInput()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new YourTurn()))));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Busy()))));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Unknown()))));

        verify(notifications, never()).show(any());
    }

    @Test
    void shouldDoNothingWhenAReadChangesNoState() {
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verifyNoMoreInteractions(notifications);
    }

    @Test
    void shouldFireForASessionThatAppearsAlreadyWaitingAfterTheFirstRead() {
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings);

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verify(notifications).show(inputNotification());
    }

    @Test
    void shouldNameTheProjectOfASessionElsewhereInTheTitle() {
        when(notifications.show(otherProjectNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings);

        manager.seen(project(), elsewhereOf(List.of(otherProjectGroup(List.of(sessionWith(waitingForInput()))))));

        verify(notifications).show(otherProjectNotification());
    }

    @Test
    void shouldReplaceTheNotificationInPlaceWhenPermissionBecomesInput() {
        when(notifications.show(permissionNotification())).thenReturn(notificationId());
        when(notifications.replace(notificationId(), inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForPermission()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verify(notifications).replace(notificationId(), inputNotification());
        verify(notifications, times(1)).show(any());
    }

    @Test
    void shouldCloseTheIdTheReplacementWasGivenWhenTheSessionIsAnswered() {
        when(notifications.show(permissionNotification())).thenReturn(notificationId());
        when(notifications.replace(notificationId(), inputNotification())).thenReturn(secondNotificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForPermission()))));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Busy()))));

        verify(notifications).close(secondNotificationId());
    }

    @Test
    void shouldCloseTheNotificationWhenTheSessionIsAnswered() {
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Busy()))));

        verify(notifications).close(notificationId());
    }

    @Test
    void shouldCloseTheNotificationWhenTheSessionDies() {
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        manager.seen(project(), liveSessionsOf(List.of()));

        verify(notifications).close(notificationId());
    }

    @Test
    void shouldCloseNothingForASessionThatNeverWasNotified() {
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));

        manager.seen(project(), liveSessionsOf(List.of()));

        verifyNoInteractions(notifications);
    }

    @Test
    void shouldFireNothingWhileTheSessionIsWatched() {
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.watching(new WatchedSession(sessionId()));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verifyNoInteractions(notifications);
    }

    @Test
    void shouldCloseTheNotificationOfASessionThatBecomesWatched() {
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        manager.watching(new WatchedSession(sessionId()));

        verify(notifications).close(notificationId());
    }

    @Test
    void shouldFireNothingForAWatchedSessionThatStopsBeingWatchedWhileStillWaiting() {
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.watching(new WatchedSession(sessionId()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        manager.watching(new NoWatchedSession());
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verifyNoInteractions(notifications);
    }

    @Test
    void shouldReplaceNothingForAWatchedSessionWhosePromptChanges() {
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.watching(new WatchedSession(sessionId()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForPermission()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verifyNoInteractions(notifications);
    }

    @Test
    void shouldIgnoreSessionsThatCouldNotBeRead() {
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));

        manager.seen(project(), unavailableSessions());
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        verify(notifications).show(inputNotification());
    }

    @Test
    void shouldWarnOnceHoweverOftenTheDesktopFails() {
        when(notifications.show(any())).thenThrow(new DesktopNotificationUnavailableException("notify-send is not installed"));
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Busy()))));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        assertThat(raisedTexts(warnings)).containsExactly("notifications: notify-send is not installed.");
    }

    @Test
    void shouldWarnWhenReplacingFails() {
        when(notifications.show(any())).thenReturn(notificationId());
        doThrow(new DesktopNotificationUnavailableException("the bus is gone")).when(notifications).replace(any(), any());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForPermission()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        assertThat(raisedTexts(warnings)).containsExactly("notifications: the bus is gone.");
    }

    @Test
    void shouldWarnWhenClosingFailsAndKeepGoing() {
        when(notifications.show(any())).thenReturn(notificationId());
        doThrow(new DesktopNotificationUnavailableException("gdbus is not installed")).when(notifications).close(any());
        DesktopNotificationManager manager = managerHavingSeen(notifications, warnings, sessionWith(new Busy()));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(waitingForInput()))));

        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Busy()))));
        manager.seen(project(), liveSessionsOf(List.of(sessionWith(new Busy()))));

        assertThat(raisedTexts(warnings)).containsExactly("notifications: gdbus is not installed.");
        verify(notifications, times(1)).close(any());
    }
}

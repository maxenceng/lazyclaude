package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;

import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class DesktopNotificationFixture {

    private DesktopNotificationFixture() {}

    public static DesktopNotification inputNotification() {
        return DesktopNotification.builder()
                .title(new NotificationTitle("app · lazyclaude-b6"))
                .body(new NotificationBody("input needed"))
                .build();
    }

    public static NotificationId notificationId() {
        return new NotificationId("42");
    }

    public static DesktopNotification permissionNotification() {
        return DesktopNotification.builder()
                .title(new NotificationTitle("app · lazyclaude-b6"))
                .body(new NotificationBody("permission prompt"))
                .build();
    }

    public static DesktopNotification otherProjectNotification() {
        return DesktopNotification.builder()
                .title(new NotificationTitle("other · lazyclaude-b6"))
                .body(new NotificationBody("input needed"))
                .build();
    }

    public static NotificationId secondNotificationId() {
        return new NotificationId("43");
    }

    public static DesktopNotificationManager managerHavingSeen(
            DesktopNotificationPort notifications, WarningPort warnings, LiveSession... sessions) {
        DesktopNotificationManager manager = new DesktopNotificationManager(notifications, warnings);
        manager.seen(project(), liveSessionsOf(List.of(sessions)), true);
        return manager;
    }
}

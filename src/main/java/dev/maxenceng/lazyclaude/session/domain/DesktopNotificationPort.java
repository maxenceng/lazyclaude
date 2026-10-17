package dev.maxenceng.lazyclaude.session.domain;

/** Where notifications reach the desktop. */
public interface DesktopNotificationPort {

    /**
     * Shows {@code notification} and answers the id the desktop gave it.
     *
     * @throws DesktopNotificationUnavailableException when it could not be shown, with the reason
     */
    NotificationId show(DesktopNotification notification);

    /**
     * Shows {@code notification} in place of the one {@code id} named, so no second one appears.
     *
     * @throws DesktopNotificationUnavailableException when it could not be replaced, with the reason
     */
    void replace(NotificationId id, DesktopNotification notification);

    /**
     * Takes the notification {@code id} named off the screen.
     *
     * @throws DesktopNotificationUnavailableException when it could not be closed, with the reason
     */
    void close(NotificationId id);
}

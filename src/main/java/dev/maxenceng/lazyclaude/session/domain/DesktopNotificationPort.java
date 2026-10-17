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
     * Shows {@code notification} in place of the one {@code id} named, so no second one appears, and answers the id it now has:
     * a new one when the desktop no longer had {@code id}, because the user dismissed it.
     *
     * @throws DesktopNotificationUnavailableException when it could not be replaced, with the reason
     */
    NotificationId replace(NotificationId id, DesktopNotification notification);

    /**
     * Takes the notification {@code id} named off the screen.
     *
     * @throws DesktopNotificationUnavailableException when it could not be closed, with the reason
     */
    void close(NotificationId id);
}

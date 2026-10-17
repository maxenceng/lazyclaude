package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a desktop notification shows for a session that waits on you. */
public record DesktopNotification(NotificationTitle title, NotificationBody body) {

    public DesktopNotification {
        Assert.notNull("title", title);
        Assert.notNull("body", body);
    }

    public static DesktopNotificationBuilder builder() {
        return new DesktopNotificationBuilder();
    }

    public static final class DesktopNotificationBuilder {
        private NotificationTitle title;
        private NotificationBody body;

        private DesktopNotificationBuilder() {}

        public DesktopNotificationBuilder title(NotificationTitle title) {
            this.title = title;
            return this;
        }

        public DesktopNotificationBuilder body(NotificationBody body) {
            this.body = body;
            return this;
        }

        public DesktopNotification build() {
            return new DesktopNotification(title, body);
        }
    }
}

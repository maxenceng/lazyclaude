package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The id the desktop gave a notification, by which it is replaced and closed. */
public record NotificationId(String value) {

    public NotificationId {
        Assert.notBlank("notification id", value);
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The heading of a desktop notification: the place and the session. */
public record NotificationTitle(String value) {

    public NotificationTitle {
        Assert.notBlank("notification title", value);
    }
}

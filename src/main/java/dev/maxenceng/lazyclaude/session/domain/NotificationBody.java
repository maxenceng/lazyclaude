package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a desktop notification says: why the session waits. */
public record NotificationBody(String value) {

    public NotificationBody {
        Assert.notBlank("notification body", value);
    }
}

package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A hosted session is running, as the session {@code id}. */
public record StartedHost(SessionId id) implements HostStart {

    public StartedHost {
        Assert.notNull("session id", id);
    }
}

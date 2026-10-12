package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A hosted session is running, under {@code id}. */
public record StartedHost(HostedSessionId id) implements HostStart {

    public StartedHost {
        Assert.notNull("hosted session id", id);
    }
}

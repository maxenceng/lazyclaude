package dev.maxenceng.lazyclaude.session.published;

import java.util.UUID;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Names a hosted session from the moment it is spawned, before Claude Code has given it a session id. */
public record HostedSessionId(UUID value) {

    public HostedSessionId {
        Assert.notNull("hosted session id", value);
    }

    public static HostedSessionId random() {
        return new HostedSessionId(UUID.randomUUID());
    }
}

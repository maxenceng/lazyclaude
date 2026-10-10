package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The id Claude Code gives a session; it stays the same while the session lives, whatever its process does. */
public record SessionId(String value) {

    public SessionId {
        Assert.notBlank("session id", value);
    }
}

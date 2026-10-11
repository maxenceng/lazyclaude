package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The id Claude Code gives a session; it stays the same while the session lives, whatever its process does. */
public record SessionId(String value) {

    private static final int SHORT_LENGTH = 8;

    public SessionId {
        Assert.notBlank("session id", value);
    }

    /** The first characters of the id, enough to tell sessions apart on a row that has nothing else to say. */
    public String shortValue() {
        return value.substring(0, Math.min(SHORT_LENGTH, value.length()));
    }
}

package dev.maxenceng.lazyclaude.session.published;

import java.util.UUID;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The id of a Claude Code session, chosen by lazyclaude for one it starts and by Claude Code otherwise; it stays the same while the session lives, whatever its process does. */
public record SessionId(String value) {

    private static final int SHORT_LENGTH = 8;

    public SessionId {
        Assert.notBlank("session id", value);
    }

    /** A fresh id for a session lazyclaude is about to start, in the form Claude Code names its sessions. */
    public static SessionId random() {
        return new SessionId(UUID.randomUUID().toString());
    }

    /** The first characters of the id, enough to tell sessions apart on a row that has nothing else to say. */
    public String shortValue() {
        return value.substring(0, Math.min(SHORT_LENGTH, value.length()));
    }
}

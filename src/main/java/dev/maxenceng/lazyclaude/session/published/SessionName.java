package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What Claude Code calls a session, shown on its row. */
public record SessionName(String value) {

    public SessionName {
        Assert.notBlank("session name", value);
    }
}

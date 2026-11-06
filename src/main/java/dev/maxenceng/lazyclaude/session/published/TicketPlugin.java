package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The Claude Code plugin whose {@code ticket} skill a ticket's new session runs. */
public record TicketPlugin(String value) {

    public TicketPlugin {
        Assert.notBlank("ticket plugin", value);
    }
}

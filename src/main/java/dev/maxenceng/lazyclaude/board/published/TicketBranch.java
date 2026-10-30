package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The branch a ticket's work happens on. */
public record TicketBranch(String value) {

    public TicketBranch {
        Assert.notBlank("ticket branch", value);
    }
}

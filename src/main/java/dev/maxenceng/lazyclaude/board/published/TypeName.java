package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The `type:` label a ticket carries: `feature`, `fix`, `chore` and so on. */
public record TypeName(String value) implements TicketType {

    public TypeName {
        Assert.notBlank("type", value);
    }
}

package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The `type` a ticket's frontmatter names: `feature`, `fix`, `chore` and so on. */
public record TypeName(String value) implements TicketType {

    public TypeName {
        Assert.notBlank("type", value);
    }
}

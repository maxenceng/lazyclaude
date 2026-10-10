package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The `context` a ticket's frontmatter names: a bounded context, `cross-context` or `unassigned`. */
public record ContextName(String value) implements TicketContext {

    public ContextName {
        Assert.notBlank("context", value);
    }
}

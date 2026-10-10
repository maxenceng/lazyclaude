package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket's markdown after its frontmatter, as written. */
public record Body(String value) implements TicketBody {

    public Body {
        Assert.notBlank("body", value);
    }
}

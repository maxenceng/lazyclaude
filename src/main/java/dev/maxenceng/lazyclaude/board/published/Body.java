package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket's issue body, as written: its `##` sections as markdown. */
public record Body(String value) implements TicketBody {

    public Body {
        Assert.notBlank("body", value);
    }
}

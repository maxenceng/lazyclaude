package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket's heading, without the `# `. */
public record Title(String value) implements TicketTitle {

    public Title {
        Assert.notBlank("title", value);
    }
}

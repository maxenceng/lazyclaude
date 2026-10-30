package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket's title: its issue's. */
public record Title(String value) {

    public Title {
        Assert.notBlank("title", value);
    }
}

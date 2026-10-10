package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A key of a ticket's frontmatter, as written. */
public record FrontmatterKey(String name) {

    public FrontmatterKey {
        Assert.notBlank("frontmatter key", name);
    }
}

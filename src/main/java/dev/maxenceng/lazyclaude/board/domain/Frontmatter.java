package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Every key a ticket's file has, in file order; the domain interprets none beyond the ones it models. */
public record Frontmatter(List<FrontmatterEntry> entries) {

    public Frontmatter {
        Assert.notNull("entries", entries);
    }
}

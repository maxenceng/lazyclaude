package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The keys of a ticket's file that the board does not model as fields, in file order; the domain interprets none of them. */
public record Frontmatter(List<FrontmatterEntry> entries) {

    public Frontmatter {
        Assert.notNull("entries", entries);
    }
}

package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a frontmatter key shows: a scalar as written, a list as `[A, B]`. Empty when the key has nothing after it. */
public record FrontmatterValue(String text) {

    public FrontmatterValue {
        Assert.notNull("frontmatter value", text);
    }
}

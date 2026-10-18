package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The thread shows under the diff line at {@code index}. */
public record OnDiffLine(DiffLineIndex index) implements ThreadLine {

    public OnDiffLine {
        Assert.notNull("index", index);
    }
}

package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The commit a pull request's head branch pointed at when GitHub merged it. */
public record HeadCommit(String value) implements MergedHead {

    public HeadCommit {
        Assert.notBlank("head commit", value);
    }
}

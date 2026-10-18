package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** GitHub's id of a review thread, which a reply is addressed to. */
public record ThreadId(String value) {

    public ThreadId {
        Assert.notBlank("thread id", value);
    }
}

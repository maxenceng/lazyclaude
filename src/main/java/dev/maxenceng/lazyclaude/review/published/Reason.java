package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why GitHub could not be read, for the open pull requests, a diff or its threads, written for the person reading the screen. */
public record Reason(String value) {

    public Reason {
        Assert.notBlank("reason", value);
    }
}

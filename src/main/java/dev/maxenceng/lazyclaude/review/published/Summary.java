package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The pending review's body, as written; empty while nothing is written. */
public record Summary(String value) {

    public Summary {
        Assert.notNull("summary", value);
    }

    /** The first line, which is all the Review panel has room for. */
    public String firstLine() {
        return value.lines().findFirst().orElse("");
    }
}

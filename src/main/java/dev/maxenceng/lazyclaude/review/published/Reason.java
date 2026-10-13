package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why pull requests could not be read, written for the person reading the screen. */
public record Reason(String value) {

    public Reason {
        Assert.notBlank("reason", value);
    }
}

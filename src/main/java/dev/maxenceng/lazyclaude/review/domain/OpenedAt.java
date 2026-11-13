package dev.maxenceng.lazyclaude.review.domain;

import java.time.Instant;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The moment a pull request was opened on GitHub; how long ago is the reader's to work out. */
public record OpenedAt(Instant at) {

    public OpenedAt {
        Assert.notNull("opened at", at);
    }
}

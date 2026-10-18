package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** The threads could not be read, and why. */
public record UnavailableThreads(Reason reason) implements ThreadsReading {

    public UnavailableThreads {
        Assert.notNull("reason", reason);
    }
}

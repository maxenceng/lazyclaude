package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** The pending review could not be read, and why. */
public record UnavailablePendingReview(Reason reason) implements PendingReviewReading {

    public UnavailablePendingReview {
        Assert.notNull("reason", reason);
    }
}

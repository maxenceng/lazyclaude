package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** GitHub took the write; {@code pending} is the pending review read again after it. */
public record WrittenReview(PendingReviewReading pending) implements PendingReviewWrite {

    public WrittenReview {
        Assert.notNull("pending", pending);
    }
}

package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;

public final class PendingReviewManagerFixture {

    private PendingReviewManagerFixture() {}

    public static UnavailablePendingReview unavailablePendingReview() {
        return new UnavailablePendingReview(reason());
    }

    public static RefusedWrite refusedWrite() {
        return new RefusedWrite(reason());
    }
}

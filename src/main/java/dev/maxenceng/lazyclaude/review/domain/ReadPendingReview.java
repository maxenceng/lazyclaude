package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The pull request as GitHub answered with it, with your pending review of it or none. */
public record ReadPendingReview(ReviewedPullRequest pullRequest) implements PendingReviewReading {

    public ReadPendingReview {
        Assert.notNull("pull request", pullRequest);
    }
}

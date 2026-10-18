package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** GitHub's id of a pull request review, which comments are added to and which is submitted. */
public record PendingReviewId(String value) {

    public PendingReviewId {
        Assert.notBlank("pending review id", value);
    }
}

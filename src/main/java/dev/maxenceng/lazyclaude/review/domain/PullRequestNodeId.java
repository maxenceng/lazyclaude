package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** GitHub's id of a pull request, which a review is created on. */
public record PullRequestNodeId(String value) {

    public PullRequestNodeId {
        Assert.notBlank("pull request id", value);
    }
}

package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The branch a pull request is opened from; a ticket's pull request is the one whose head is its ticket branch. */
public record HeadBranch(String value) {

    public HeadBranch {
        Assert.notBlank("head branch", value);
    }
}

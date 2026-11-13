package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The first issue of this repository that the pull request closes. */
public record ClosesIssue(ClosingIssueNumber number) implements IssueClosing {

    public ClosesIssue {
        Assert.notNull("closing issue number", number);
    }
}

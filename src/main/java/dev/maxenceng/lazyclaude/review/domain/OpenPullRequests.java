package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The project's open pull requests, draft ones included. */
public record OpenPullRequests(List<PullRequest> pullRequests) implements PullRequests {

    public OpenPullRequests {
        Assert.notNull("pull requests", pullRequests);
    }
}

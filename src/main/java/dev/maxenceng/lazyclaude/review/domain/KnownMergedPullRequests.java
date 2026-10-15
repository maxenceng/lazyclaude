package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The most recent merged pull requests of the project's repository, as many as one fetch asks for. */
public record KnownMergedPullRequests(List<MergedPullRequest> pullRequests) implements MergedPullRequests {

    public KnownMergedPullRequests {
        Assert.notNull("pull requests", pullRequests);
    }
}

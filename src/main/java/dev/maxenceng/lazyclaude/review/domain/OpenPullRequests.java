package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;

/** The project's open pull requests, draft ones included. */
public record OpenPullRequests(List<PullRequest> pullRequests) implements PullRequests {

    public OpenPullRequests {
        Assert.notNull("pull requests", pullRequests);
    }

    /** The pull request whose head is exactly {@code branch}: a head that only starts with it is another branch's. */
    public Optional<PullRequest> on(HeadBranch branch) {
        return pullRequests.stream().filter(pullRequest -> pullRequest.head().equals(branch)).findFirst();
    }
}

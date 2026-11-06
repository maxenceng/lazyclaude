package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.HeadBranch;

/** What {@link ReviewQuery} is asked a branch's pull request with: the open pull requests as last pushed, and the branch. */
public record PullRequestOnPayload(PullRequestsView pullRequests, HeadBranch branch) {}

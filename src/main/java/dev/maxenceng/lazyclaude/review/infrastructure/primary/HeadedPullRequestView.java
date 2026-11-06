package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** The open pull request whose head is the branch. */
public record HeadedPullRequestView(PullRequestView pullRequest) implements PullRequestOnView {}

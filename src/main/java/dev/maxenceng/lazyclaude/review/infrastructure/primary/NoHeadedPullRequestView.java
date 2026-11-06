package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** No open pull request has the branch as its head, or the open ones are not known. */
public record NoHeadedPullRequestView() implements PullRequestOnView {}

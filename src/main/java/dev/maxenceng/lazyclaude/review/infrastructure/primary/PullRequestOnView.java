package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** A branch's pull request: the open one whose head it is, or none — none open, or the pull requests not fetched or unreadable. */
public sealed interface PullRequestOnView permits HeadedPullRequestView, NoHeadedPullRequestView {}

package dev.maxenceng.lazyclaude.review.domain;

/** What a fetch of the open pull requests came to: not answered yet, the open ones, or the reason they could not be read. */
public sealed interface PullRequests permits CheckingPullRequests, OpenPullRequests, UnavailablePullRequests {}

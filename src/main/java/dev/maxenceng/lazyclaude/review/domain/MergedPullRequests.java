package dev.maxenceng.lazyclaude.review.domain;

/** What a fetch of the merged pull requests came to: the most recent merged ones, or the reason they could not be read. */
public sealed interface MergedPullRequests permits KnownMergedPullRequests, UnavailableMergedPullRequests {}

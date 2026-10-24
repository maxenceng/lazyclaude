package dev.maxenceng.lazyclaude.review.domain;

/** Your review of a pull request that is not yet submitted, or nothing when there is none — never a stand-in. */
public sealed interface ReviewInProgress permits PendingReview, NoPendingReview {}

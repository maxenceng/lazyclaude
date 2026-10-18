package dev.maxenceng.lazyclaude.review.domain;

/** What a read of a pull request's pending review came to: not answered yet, the pull request with your review of it, or why it could not be read. */
public sealed interface PendingReviewReading permits LoadingPendingReview, ReadPendingReview, UnavailablePendingReview {}

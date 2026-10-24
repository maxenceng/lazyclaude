package dev.maxenceng.lazyclaude.review.domain;

/** What a write to the pending review came to: GitHub took it, or it did not. */
public sealed interface PendingReviewWrite permits WrittenReview, RefusedWrite {}

package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

/** A pending review as the pending review query writes it. */
record PendingReviewRecord(String id, String body, PendingReviewCommitRecord commit) {}

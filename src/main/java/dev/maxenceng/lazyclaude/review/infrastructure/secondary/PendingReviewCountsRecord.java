package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

/** The pending reviews GitHub shows you on a pull request: yours, at most one; null where the JSON lacks them. */
record PendingReviewCountsRecord(List<PendingReviewCountRecord> nodes) {}

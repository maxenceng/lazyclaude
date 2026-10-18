package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

/** The pending reviews of the pull request as the pending review query writes them: at most the one of the person asking. */
record PendingReviewConnectionRecord(List<PendingReviewRecord> nodes) {}

package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

/** The pull request of a page of the threads query, or of the pending review query, which asks for its id, head and review as well. */
record ThreadPagePullRequestRecord(
        String id, String headRefOid, PendingReviewConnectionRecord reviews, ThreadConnectionRecord reviewThreads) {}

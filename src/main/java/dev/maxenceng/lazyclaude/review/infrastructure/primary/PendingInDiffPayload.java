package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** What {@link ReviewQuery} is asked the pending comments with their diff lines with: the diff and the pending review, as whatever each read last answered. */
public record PendingInDiffPayload(PullRequestDiffView diff, PendingReviewView pending) {}

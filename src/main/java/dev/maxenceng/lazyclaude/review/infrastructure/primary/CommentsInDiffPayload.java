package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/**
 * What {@link ReviewQuery} is asked a pull request's comments with: the diff, the threads and the pending review, as whatever each read
 * last answered, and whether resolved threads show under their lines.
 */
public record CommentsInDiffPayload(PullRequestDiffView diff, ThreadsView threads, PendingReviewView pending, boolean resolvedShown) {}

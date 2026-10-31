package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** What {@link ReviewQuery} is asked the threads with their diff lines with: the diff and the threads, as whatever each read last answered. */
public record ThreadsInDiffPayload(PullRequestDiffView diff, ThreadsView threads) {}

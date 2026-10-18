package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ThreadsInDiff;

/** The threads in diff order, each with its line: by the line they show under, the ones on no line last, by file and line. */
public record ThreadsInDiffView(List<ThreadInDiffView> threads) {

    static ThreadsInDiffView from(ThreadsInDiff placed) {
        return new ThreadsInDiffView(placed.threads().stream().map(ThreadInDiffView::from).toList());
    }
}

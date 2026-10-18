package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ReadThreads;

/** The threads, in diff order: by file, then by line, the outdated ones last. */
public record ReadThreadsView(List<ReviewThreadView> threads) implements ThreadsView {



    static ReadThreadsView from(ReadThreads read) {
        return new ReadThreadsView(read.inDiffOrder().stream().map(ReviewThreadView::from).toList());
    }
}

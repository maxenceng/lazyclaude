package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ReadThreads;

/** The threads, by file and then line number, the outdated ones last; {@link ReviewQuery#threadsInDiff} puts them in diff order. */
public record ReadThreadsView(List<ReviewThreadView> threads) implements ThreadsView {



    static ReadThreadsView from(ReadThreads read) {
        return new ReadThreadsView(read.byFileAndLine().stream().map(ReviewThreadView::from).toList());
    }
}

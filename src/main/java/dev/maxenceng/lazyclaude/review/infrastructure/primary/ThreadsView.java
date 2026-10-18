package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.LoadingThreads;
import dev.maxenceng.lazyclaude.review.domain.ReadThreads;
import dev.maxenceng.lazyclaude.review.domain.ThreadsReading;
import dev.maxenceng.lazyclaude.review.domain.UnavailableThreads;

/** A pull request's threads as the shell reads them: not read yet, the threads in diff order, or why GitHub could not be asked for them. */
public sealed interface ThreadsView permits LoadingThreadsView, ReadThreadsView, UnavailableThreadsView {

    static ThreadsView from(ThreadsReading reading) {
        return switch (reading) {
            case LoadingThreads _ -> new LoadingThreadsView();
            case ReadThreads read -> ReadThreadsView.from(read);
            case UnavailableThreads unavailable -> new UnavailableThreadsView(unavailable.reason());
        };
    }
}

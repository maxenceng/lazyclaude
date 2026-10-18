package dev.maxenceng.lazyclaude.review.domain;

import java.util.Comparator;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;

/** The threads GitHub answered with. */
public record ReadThreads(List<ReviewThread> threads) implements ThreadsReading {

    private static final Comparator<ReviewThread> IN_DIFF_ORDER = Comparator.comparing(ReadThreads::isOutdated)
            .thenComparing(thread -> thread.path().value())
            .thenComparingInt(ReadThreads::lineShownUnder);

    public ReadThreads {
        Assert.notNull("threads", threads);
    }

    /** The threads by file, then by the line they show under, the outdated ones, which sit on no line, last. */
    public List<ReviewThread> inDiffOrder() {
        return threads.stream().sorted(IN_DIFF_ORDER).toList();
    }

    private static boolean isOutdated(ReviewThread thread) {
        return !(thread.position() instanceof PlacedThread);
    }

    private static int lineShownUnder(ReviewThread thread) {
        return thread.position() instanceof PlacedThread placed ? placed.last().value() : 0;
    }
}

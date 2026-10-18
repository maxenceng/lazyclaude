package dev.maxenceng.lazyclaude.review.domain;

import java.util.Comparator;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;

/** The threads GitHub answered with. */
public record ReadThreads(List<ReviewThread> threads) implements ThreadsReading {

    private static final Comparator<ReviewThread> BY_FILE_AND_LINE = Comparator.comparing(ReadThreads::isOutdated)
            .thenComparing(thread -> thread.path().value())
            .thenComparingInt(ReadThreads::lineShownUnder);

    public ReadThreads {
        Assert.notNull("threads", threads);
    }

    /**
     * The threads by file, then by the number of the line they show under whichever side it is on, the outdated ones, which sit on no line,
     * last. Only the diff knows which line a number is; {@link PullRequestDiff#place} turns this into diff order.
     */
    public List<ReviewThread> byFileAndLine() {
        return threads.stream().sorted(BY_FILE_AND_LINE).toList();
    }

    private static boolean isOutdated(ReviewThread thread) {
        return !(thread.position() instanceof PlacedThread);
    }

    private static int lineShownUnder(ReviewThread thread) {
        return thread.position() instanceof PlacedThread placed ? placed.last().value() : 0;
    }
}

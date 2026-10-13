package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;

public final class PullRequestWatchFixture {

    private static final Duration SHORT_INTERVAL = Duration.ofMillis(100);
    private static final Duration LONG_INTERVAL = Duration.ofMinutes(5);
    private static final Duration SLOW_RUN = Duration.ofMillis(300);

    private PullRequestWatchFixture() {}

    public static Duration within() {
        return Duration.ofSeconds(5);
    }

    public static Duration settled() {
        return Duration.ofMillis(700);
    }

    static PullRequestWatchRepository watcherEveryFewMillis() {
        return new PullRequestWatchRepository(SHORT_INTERVAL);
    }

    static PullRequestWatchRepository watcherWithoutAnotherRun() {
        return new PullRequestWatchRepository(LONG_INTERVAL);
    }

    static RefreshProbe instantRuns() {
        return new RefreshProbe(Duration.ZERO);
    }

    static RefreshProbe runsLongerThanTheInterval() {
        return new RefreshProbe(SLOW_RUN);
    }
}

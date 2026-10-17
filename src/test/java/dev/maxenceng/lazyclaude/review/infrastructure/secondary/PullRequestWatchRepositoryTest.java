package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.PullRequestWatchFixture.instantRuns;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.PullRequestWatchFixture.runsLongerThanTheInterval;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.PullRequestWatchFixture.settled;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.PullRequestWatchFixture.watcherEveryFewMillis;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.PullRequestWatchFixture.watcherWithoutAnotherRun;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.PullRequestWatchFixture.within;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PullRequestWatchRepositoryTest {

    private PullRequestWatchRepository watcher;

    @AfterEach
    void stopWatching() {
        watcher.close();
    }

    @Test
    void shouldRunTheRefreshAtOnceRatherThanAfterTheFirstInterval() throws InterruptedException {
        RefreshProbe probe = instantRuns();
        watcher = watcherWithoutAnotherRun();

        watcher.watch(probe.refresh());

        assertThat(probe.nextRunWithin(within())).as("the first fetch is at startup, not 60 s later").isPresent();
    }

    @Test
    void shouldRunTheRefreshAgainAfterEachInterval() throws InterruptedException {
        RefreshProbe probe = instantRuns();
        watcher = watcherEveryFewMillis();

        watcher.watch(probe.refresh());

        assertThat(probe.nextRunWithin(within())).isPresent();
        assertThat(probe.nextRunWithin(within())).isPresent();
        assertThat(probe.nextRunWithin(within())).isPresent();
    }

    @Test
    void shouldNeverRunTwoRefreshesAtOnceWhenOneOutlastsTheInterval() throws InterruptedException {
        RefreshProbe probe = runsLongerThanTheInterval();
        watcher = watcherEveryFewMillis();

        watcher.watch(probe.refresh());
        probe.nextRunWithin(within());
        probe.nextRunWithin(within());
        probe.nextRunWithin(within());

        assertThat(probe.mostAtOnce()).as("a slow gh must not be followed by a second one").isEqualTo(1);
    }

    @Test
    void shouldRunOnADaemonThreadOfItsOwn() throws InterruptedException {
        RefreshProbe probe = instantRuns();
        watcher = watcherWithoutAnotherRun();

        watcher.watch(probe.refresh());

        Thread ran = probe.nextRunWithin(within()).orElseThrow();
        assertThat(ran).isNotSameAs(Thread.currentThread());
        assertThat(ran.isDaemon()).as("a daemon, so the watcher never keeps lazyclaude alive").isTrue();
    }

    @Test
    void shouldStopRunningOnceClosed() throws InterruptedException {
        RefreshProbe probe = instantRuns();
        watcher = watcherEveryFewMillis();
        watcher.watch(probe.refresh());
        probe.nextRunWithin(within());

        watcher.close();
        Thread.sleep(settled().toMillis());
        probe.discardRuns();

        assertThat(probe.nextRunWithin(settled())).isEmpty();
    }

    @Test
    void shouldRunTheRefreshAgainAtOnceWhenAskedToRunNow() throws InterruptedException {
        RefreshProbe probe = instantRuns();
        watcher = watcherWithoutAnotherRun();
        watcher.watch(probe.refresh());
        probe.nextRunWithin(within());

        watcher.runNow();

        assertThat(probe.nextRunWithin(within()))
                .as("the interval is five minutes, so only the request can have caused this run")
                .isPresent();
    }

    @Test
    void shouldNotStartASecondRunWhenAskedToRunNowWhileOneIsUnderWay() throws InterruptedException {
        RefreshProbe probe = instantRuns();
        watcher = watcherWithoutAnotherRun();
        watcher.watch(() -> {
            watcher.runNow();
            probe.refresh().run();
        });
        probe.nextRunWithin(within());

        assertThat(probe.nextRunWithin(settled()))
                .as("a gh already under way answers the request, so a second one is never queued behind it")
                .isEmpty();
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;

/**
 * Runs the refresh at once and then after every interval, on one daemon thread. One thread is what keeps two runs from overlapping: the
 * interval is counted from the end of a run, so a slow {@code gh} is never followed by a second one.
 */
public final class PullRequestWatchRepository implements PullRequestWatchPort, AutoCloseable {

    static final Duration INTERVAL = Duration.ofSeconds(60);

    private static final String WORKER_NAME = "pull-request-watch";

    private final Duration interval;
    private final List<Thread> workers = new CopyOnWriteArrayList<>();

    public PullRequestWatchRepository() {
        this(INTERVAL);
    }

    PullRequestWatchRepository(Duration interval) {
        this.interval = interval;
    }

    @Override
    public void watch(Runnable refresh) {
        workers.add(Thread.ofPlatform().daemon().name(WORKER_NAME).start(() -> loop(refresh)));
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
    }

    // deferred: it fetches while the terminal is unfocused too — close it when SESSION-7 decodes ?1004 focus events and the timer can pause.
    private void loop(Runnable refresh) {
        try {
            while (true) {
                refresh.run();
                Thread.sleep(interval.toMillis());
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;

/**
 * Runs the refresh at once and then after every interval, on one daemon thread. One thread is what keeps two runs from overlapping: the
 * interval is counted from the end of a run, so a slow {@code gh} is never followed by a second one. A request to run now cuts the
 * wait short, and one made while a run is under way is dropped, since that run answers it.
 */
public final class PullRequestWatchRepository implements PullRequestWatchPort, AutoCloseable {

    static final Duration INTERVAL = Duration.ofSeconds(60);

    private static final String WORKER_NAME = "pull-request-watch";

    private final Duration interval;
    private final List<Thread> workers = new CopyOnWriteArrayList<>();
    private final Semaphore requests = new Semaphore(0);
    private final AtomicBoolean running = new AtomicBoolean();

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
    public void runNow() {
        if (!running.get()) {
            requests.release();
        }
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
    }

    // deferred: it fetches while the terminal is unfocused too — close it when SESSION-7 decodes ?1004 focus events and the timer can pause.
    private void loop(Runnable refresh) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                running.set(true);
                try {
                    refresh.run();
                } finally {
                    running.set(false);
                }
                requests.tryAcquire(interval.toMillis(), TimeUnit.MILLISECONDS);
                requests.drainPermits();
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }
}

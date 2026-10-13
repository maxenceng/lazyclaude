package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class RefreshProbe {

    private final BlockingQueue<Thread> runs = new LinkedBlockingQueue<>();
    private final AtomicInteger running = new AtomicInteger();
    private final AtomicInteger mostAtOnce = new AtomicInteger();
    private final Duration eachRunTakes;

    RefreshProbe(Duration eachRunTakes) {
        this.eachRunTakes = eachRunTakes;
    }

    Runnable refresh() {
        return () -> {
            mostAtOnce.accumulateAndGet(running.incrementAndGet(), Math::max);
            sleepThroughTheRun();
            runs.add(Thread.currentThread());
            running.decrementAndGet();
        };
    }

    Optional<Thread> nextRunWithin(Duration wait) throws InterruptedException {
        return Optional.ofNullable(runs.poll(wait.toMillis(), TimeUnit.MILLISECONDS));
    }

    void discardRuns() {
        runs.clear();
    }

    int mostAtOnce() {
        return mostAtOnce.get();
    }

    private void sleepThroughTheRun() {
        try {
            Thread.sleep(eachRunTakes.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

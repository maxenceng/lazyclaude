package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class ChangeProbe {

    private final BlockingQueue<Boolean> changes = new LinkedBlockingQueue<>();

    Runnable listener() {
        return () -> changes.add(true);
    }

    boolean changedWithin(Duration wait) throws InterruptedException {
        return changes.poll(wait.toMillis(), TimeUnit.MILLISECONDS) != null;
    }

    boolean quietFor(Duration wait) throws InterruptedException {
        return !changedWithin(wait);
    }
}

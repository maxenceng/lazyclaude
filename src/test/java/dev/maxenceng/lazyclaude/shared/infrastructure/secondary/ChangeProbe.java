package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class ChangeProbe {

    private final BlockingQueue<Boolean> changes = new LinkedBlockingQueue<>();

    public Runnable listener() {
        return () -> changes.add(true);
    }

    public boolean changedWithin(Duration wait) throws InterruptedException {
        return changes.poll(wait.toMillis(), TimeUnit.MILLISECONDS) != null;
    }

    public boolean quietFor(Duration wait) throws InterruptedException {
        return !changedWithin(wait);
    }
}

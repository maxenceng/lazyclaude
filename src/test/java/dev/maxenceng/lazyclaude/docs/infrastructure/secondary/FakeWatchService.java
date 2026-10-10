package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class FakeWatchService implements WatchService {

    private final BlockingQueue<WatchKey> keys = new LinkedBlockingQueue<>();
    private volatile boolean closed;

    void offer(WatchKey key) {
        keys.add(key);
    }

    @Override
    public WatchKey take() throws InterruptedException {
        WatchKey key = keys.take();
        if (closed) {
            throw new ClosedWatchServiceException();
        }
        return key;
    }

    @Override
    public WatchKey poll() {
        return keys.poll();
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
        return keys.poll(timeout, unit);
    }

    boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}

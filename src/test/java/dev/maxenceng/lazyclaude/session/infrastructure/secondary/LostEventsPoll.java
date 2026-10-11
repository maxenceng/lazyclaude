package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

final class LostEventsPoll implements WatchKeyPoll {

    private final Path projects;
    private final AtomicBoolean overflowed = new AtomicBoolean();
    private final AtomicBoolean recovered = new AtomicBoolean();

    LostEventsPoll(Path projects) {
        this.projects = projects;
    }

    void overflow() {
        overflowed.set(true);
    }

    @Override
    public WatchKey next(WatchService service, Duration wait) throws InterruptedException {
        if (overflowed.getAndSet(false)) {
            recovered.set(true);
            return new OverflowKey(projects);
        }
        WatchKey key = WatchKeyPoll.platform().next(service, wait);
        if (key == null || recovered.get()) {
            return key;
        }
        key.pollEvents();
        key.reset();
        return null;
    }
}

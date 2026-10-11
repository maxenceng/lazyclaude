package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Takes the next signalled key off the transcript watch; a seam so an {@code OVERFLOW}, which a real watch raises only
 * under load, can be delivered on demand. Registration stays on the platform's service, which is all it accepts.
 */
@FunctionalInterface
interface WatchKeyPoll {

    WatchKey next(WatchService service, Duration wait) throws InterruptedException;

    static WatchKeyPoll platform() {
        return (service, wait) -> service.poll(wait.toMillis(), TimeUnit.MILLISECONDS);
    }
}

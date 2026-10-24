package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.util.Objects;

/** Whether a watcher has a {@link java.nio.file.WatchService} to wait on, or why it has none and checks on a timer instead. */
public sealed interface WatchServiceState permits Watching, NotWatching {

    /** Watching the service {@code services} makes, else not watching for the reason it could not. */
    static WatchServiceState of(WatchServiceFactory services) {
        try {
            return new Watching(services.create());
        } catch (IOException e) {
            return new NotWatching(Objects.requireNonNullElseGet(e.getMessage(), () -> e.getClass().getSimpleName()));
        }
    }
}

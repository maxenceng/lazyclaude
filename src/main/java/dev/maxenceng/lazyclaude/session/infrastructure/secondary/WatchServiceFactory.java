package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.WatchService;

/** Makes the {@link WatchService} the session watcher registers on; a seam so a machine without inotify can be simulated. */
@FunctionalInterface
interface WatchServiceFactory {

    WatchService create() throws IOException;
}

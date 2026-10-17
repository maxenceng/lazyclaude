package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.WatchService;

/** Makes the {@link WatchService} a tree watcher registers on; a seam so a machine without inotify can be simulated. */
@FunctionalInterface
public interface WatchServiceFactory {

    WatchService create() throws IOException;
}

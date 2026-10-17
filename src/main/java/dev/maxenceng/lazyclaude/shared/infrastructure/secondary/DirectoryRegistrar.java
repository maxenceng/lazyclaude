package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/** Puts one directory under a {@link WatchService}; a seam so a registration that fails partway can be simulated. */
@FunctionalInterface
public interface DirectoryRegistrar {

    WatchKey register(WatchService service, Path directory) throws IOException;
}

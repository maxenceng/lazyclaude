package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Path;

/** The docs tree cannot be put under a watch, at start or while a folder is added; the watcher polls instead. */
final class WatchUnavailableException extends RuntimeException {

    WatchUnavailableException(Path docs, IOException cause) {
        super("cannot watch %s (%s)".formatted(docs, cause.getMessage()), cause);
    }
}

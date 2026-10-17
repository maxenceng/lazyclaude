package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.nio.file.Path;

/** What one tree watch covers: where it starts, which directories get a watch, and which changes are worth a report. */
public interface TreeScope {

    /** The directory the watch starts from; it is always watched. */
    Path root();

    /** Whether {@code directory}, found under the root, gets a watch of its own; a directory that does not is skipped with everything beneath it. */
    boolean descends(Path directory);

    /** Whether a change to {@code entry}, a file or a directory in a watched one, is reported. */
    boolean counts(Path entry);
}

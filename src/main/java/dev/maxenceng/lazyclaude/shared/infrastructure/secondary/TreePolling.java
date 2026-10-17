package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Duration;

/** What a tree watcher tells its owner when a tree cannot be watched and is checked on a timer instead. */
@FunctionalInterface
public interface TreePolling {

    /** Said once for each tree that is given up, with the whole cause in the reader's words and how often the tree is checked now. */
    void polling(Path tree, String cause, Duration every);
}

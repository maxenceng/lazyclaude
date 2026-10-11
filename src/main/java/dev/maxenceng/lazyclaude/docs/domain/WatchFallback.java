package dev.maxenceng.lazyclaude.docs.domain;

import java.time.Duration;

/** What a watcher tells the domain when it cannot watch the docs and checks them on a timer instead. */
@FunctionalInterface
public interface WatchFallback {

    /** Said once, naming why the watch could not be set up and how often the docs are checked now. */
    void polling(String cause, Duration every);
}

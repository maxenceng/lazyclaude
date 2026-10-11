package dev.maxenceng.lazyclaude.docs.domain;

import java.time.Duration;

import dev.maxenceng.lazyclaude.docs.published.Reason;

/** What a watcher tells the domain when it cannot watch the docs and checks them on a timer instead. */
@FunctionalInterface
public interface WatchFallback {

    /** Said each time the watch is given up, with the whole cause in the reader's words and how often the docs are checked now. */
    void polling(Reason cause, Duration every);
}

package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;

/** What a watcher tells the domain when it cannot watch the session registry and sweeps it on a timer alone. */
@FunctionalInterface
public interface WatchFallback {

    /** Said once, naming why the registry could not be watched and how often it is swept. */
    void sweepingAlone(String cause, Duration every);
}

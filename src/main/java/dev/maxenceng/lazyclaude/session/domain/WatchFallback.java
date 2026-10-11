package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** What a watcher tells the domain when it cannot watch the session registry and sweeps it on a timer alone. */
@FunctionalInterface
public interface WatchFallback {

    /** Said each time the watch is given up, with the whole cause in the reader's words and how often the registry is swept. */
    void sweepingAlone(Reason cause, Duration every);
}

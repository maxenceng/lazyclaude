package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** What the transcript watcher tells the domain when it cannot watch the transcripts and checks them on a timer instead. */
@FunctionalInterface
public interface PollingFallback {

    /** Said each time the watch is given up, with the whole cause in the reader's words and how often the transcripts are checked now. */
    void polling(Reason cause, Duration every);
}

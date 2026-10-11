package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** What a watcher tells the domain when it cannot write the cache of what it read, so every start reads the transcripts in full. */
@FunctionalInterface
public interface CacheFallback {

    /** Said each time a write fails, with the whole cause in the reader's words. */
    void unwritable(Reason cause);
}

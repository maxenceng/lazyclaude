package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** What a watcher tells the domain when the transcripts root is there but cannot be listed, so only cached past sessions show. */
@FunctionalInterface
public interface RootFallback {

    /** Said each time a listing fails, with the whole cause in the reader's words. */
    void unlistable(Reason cause);
}

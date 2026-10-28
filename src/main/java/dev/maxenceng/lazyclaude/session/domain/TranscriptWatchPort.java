package dev.maxenceng.lazyclaude.session.domain;

import java.util.Set;
import java.util.function.Supplier;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** Keeps the transcripts of the logged sessions current and tells the domain when they may have changed. */
public interface TranscriptWatchPort {

    /**
     * Starts reading the transcripts of the sessions {@code logged} names that are new or changed since the cache, off the
     * caller's thread, and runs {@code changed} on a thread of the watcher's own once each batch of rescans has landed. A call does
     * not say what changed, and a transcript of a session not logged then is not read. When the transcripts cannot be watched and
     * are polled on a timer instead, {@code fallback} is told once; when the cache cannot be written, {@code cache} is; when the
     * transcripts root is there but cannot be listed, {@code root} is.
     */
    void watch(
            Supplier<Set<SessionId>> logged,
            Runnable changed,
            PollingFallback fallback,
            CacheFallback cache,
            RootFallback root);

    /** Reads every logged transcript again soon, whether or not anything seems to have changed, and runs {@code changed} once they have landed. */
    void rescanNow();
}

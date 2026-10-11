package dev.maxenceng.lazyclaude.session.domain;

/** Tells the domain when the machine's sessions may have changed. */
public interface SessionWatchPort {

    /**
     * Runs {@code changed} on a thread of the watcher's own: at once after Claude Code rewrites a record, and on a
     * short sweep besides, for as long as the process lives. A call does not say what changed. When the registry cannot be
     * watched and the sweep carries on alone, {@code fallback} is told once.
     */
    void watch(Runnable changed, WatchFallback fallback);
}

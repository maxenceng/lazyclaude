package dev.maxenceng.lazyclaude.session.domain;

import java.util.function.Predicate;

import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** Tells the domain when the registry record of a hosted session may have changed. */
public interface SessionWatchPort {

    /**
     * Runs {@code changed} on a thread of the watcher's own: at once after Claude Code rewrites the record of a process
     * {@code hosted} accepts, and on a short sweep besides, for as long as the process lives. A record of any other process
     * changes nothing. A call does not say what changed. When the registry cannot be watched and the sweep carries on alone,
     * {@code fallback} is told once.
     */
    void watch(Predicate<ProcessId> hosted, Runnable changed, WatchFallback fallback);
}

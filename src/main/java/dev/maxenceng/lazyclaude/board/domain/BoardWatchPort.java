package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Tells the domain when something the board is read from changed: the backlog, a ticket worktree's copy of a ticket, or the refs. */
public interface BoardWatchPort {

    /**
     * Runs {@code changed} after each batch of changes, on a thread of the watcher's own, for as long as the process lives. A call does
     * not say what changed. A tree that cannot be watched is polled instead, and {@code fallback} is told once for that tree.
     */
    void watch(Project project, Runnable changed, WatchFallback fallback);
}

package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Tells the domain when the project's issues changed on GitHub. */
public interface BoardWatchPort {

    /**
     * Runs {@code changed} on a thread of the watcher's own, for as long as the process lives, whenever a check every 30 s finds the
     * issues changed, or cannot tell. A call does not say what changed.
     */
    void watch(Project project, Runnable changed);
}

package dev.maxenceng.lazyclaude.docs.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Tells the domain when something under a project's {@code docs/} (outside {@code docs/backlog/}) changed. */
public interface DocsWatchPort {

    /** Runs {@code changed} after each batch of changes, on a thread of the watcher's own, for as long as the process lives. */
    void watch(Project project, Runnable changed);
}

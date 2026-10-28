package dev.maxenceng.lazyclaude.session.domain;

/** What stands in a ticket worktree: what removing it would lose or disturb, or that the merge it is judged against cannot be read. */
public sealed interface WorktreeState permits WorktreeContents, NoMergedPullRequest {

    /** Whether nothing stands in the way of removing the worktree. */
    boolean clear();
}

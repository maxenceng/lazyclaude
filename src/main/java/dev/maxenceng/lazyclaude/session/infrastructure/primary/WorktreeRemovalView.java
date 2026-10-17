package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** What asking to remove a ticket worktree came to: removed, refused for what stands in it, or failed in git. */
public sealed interface WorktreeRemovalView permits RemovedWorktreeView, RefusedRemovalView, FailedRemovalView {}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** Something stands in the worktree, so nothing was removed. */
public record RefusedRemovalView(WorktreeStateView state) implements WorktreeRemovalView {}

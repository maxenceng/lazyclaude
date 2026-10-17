package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** Git could not remove the worktree, or could not say what stands in it; the worktree and its branch are as they were. */
public record FailedRemovalView(Reason reason) implements WorktreeRemovalView {}

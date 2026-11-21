package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** The worktree and its branch are as they were: what stands in it, or what git said, worded as the terminal words it. */
public record NotRemovedWorktreeView(Reason refusal) implements WorktreeRemovalOutcomeView {}

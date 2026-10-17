package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** The ticket has no worktree at the path the project keeps them at: never created, or already removed. */
public record NoWorktreeView() implements WorktreeView {}

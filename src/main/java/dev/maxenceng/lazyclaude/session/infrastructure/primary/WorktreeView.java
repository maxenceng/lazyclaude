package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** A ticket's worktree as the shell reads it: what stands in it, that there is none, or why that could not be told. */
public sealed interface WorktreeView permits HeldWorktreeView, NoWorktreeView, UnreadableWorktreeView {}

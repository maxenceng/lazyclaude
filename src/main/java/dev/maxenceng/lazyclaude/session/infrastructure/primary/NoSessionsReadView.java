package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** The sessions could not be read, so whether one runs in the ticket worktree cannot be told. */
public record NoSessionsReadView() implements WorktreeStateView {}

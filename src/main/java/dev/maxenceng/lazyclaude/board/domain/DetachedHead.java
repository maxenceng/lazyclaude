package dev.maxenceng.lazyclaude.board.domain;

/** A worktree on a commit, with no branch checked out. */
public record DetachedHead() implements WorktreeHead {}

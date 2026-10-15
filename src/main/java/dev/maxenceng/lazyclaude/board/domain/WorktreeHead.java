package dev.maxenceng.lazyclaude.board.domain;

/** What a worktree has checked out: a branch, or a commit with no branch. */
public sealed interface WorktreeHead permits CheckedOutBranch, DetachedHead {}

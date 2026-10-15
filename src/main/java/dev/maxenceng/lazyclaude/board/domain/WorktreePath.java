package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Where a worktree is on disk. */
public record WorktreePath(Path value) {

    public WorktreePath {
        Assert.absolute("worktree path", value);
    }
}

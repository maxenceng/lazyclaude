package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The directory lazyclaude works on: the root of the main worktree. */
public record Project(Path root) {

    public Project {
        Assert.absolute("project root", root);
    }
}

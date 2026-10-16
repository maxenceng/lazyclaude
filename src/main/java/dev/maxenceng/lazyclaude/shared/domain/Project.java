package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The directory lazyclaude works on: the root of the main worktree. */
public record Project(Path root) {

    private static final String WORKTREES = ".worktrees";

    public Project {
        Assert.absolute("project root", root);
    }

    /** Where a ticket's worktree is kept: {@code ../<repo>.worktrees/<ticket branch>}; nowhere for a root that has no name. */
    public Optional<WorkingDirectory> ticketWorktree(BranchName branch) {
        return Optional.ofNullable(root.getFileName())
                .map(name -> new WorkingDirectory(root.resolveSibling(name + WORKTREES).resolve(branch.value())));
    }
}

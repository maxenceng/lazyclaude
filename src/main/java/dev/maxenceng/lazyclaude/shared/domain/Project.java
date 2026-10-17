package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.IntStream;

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

    /** The project whose ticket worktree {@code directory} lies in, read back from the {@code ../<repo>.worktrees/} path. */
    public static Optional<Project> owningTicketWorktree(WorkingDirectory directory) {
        Path path = directory.path();
        return IntStream.range(0, path.getNameCount() - 1)
                .mapToObj(segment -> path.getRoot().resolve(path.subpath(0, segment + 1)))
                .filter(Project::holdsWorktrees)
                .findFirst()
                .map(folder -> new Project(folder.resolveSibling(repositoryOf(folder))));
    }

    private static boolean holdsWorktrees(Path folder) {
        String name = folder.getFileName().toString();
        return name.endsWith(WORKTREES) && name.length() > WORKTREES.length();
    }

    private static String repositoryOf(Path worktrees) {
        String name = worktrees.getFileName().toString();
        return name.substring(0, name.length() - WORKTREES.length());
    }
}

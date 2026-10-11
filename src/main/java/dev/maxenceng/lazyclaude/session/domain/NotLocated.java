package dev.maxenceng.lazyclaude.session.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Git located the {@code cwd} in no project, typically because the directory is gone: a ticket worktree that was
 * removed. It then belongs by its path, under the project root or under {@code ../<repo>.worktrees/}.
 */
public record NotLocated() implements ProjectLocation {

    private static final String WORKTREES_SUFFIX = ".worktrees";

    @Override
    public boolean holds(Project project, WorkingDirectory directory) {
        Path path = directory.path();
        Path root = project.root();
        Path name = root.getFileName();
        return path.startsWith(root)
                || name != null && path.startsWith(root.resolveSibling(name + WORKTREES_SUFFIX));
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Git located the {@code cwd} in no project, typically because the directory is gone: a ticket worktree that was
 * removed. It then belongs by its path, under a project root or under that project's {@code ../<repo>.worktrees/}.
 */
public record NotLocated() implements ProjectLocation {

    @Override
    public boolean holds(Project project, WorkingDirectory directory) {
        return directory.path().startsWith(project.root())
                || Project.owningTicketWorktree(directory).filter(project::equals).isPresent();
    }
}

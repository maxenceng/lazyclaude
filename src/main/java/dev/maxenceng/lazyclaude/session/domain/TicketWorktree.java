package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** The worktree a ticket's branch is kept in: the project it belongs to, and the branch. */
public record TicketWorktree(Project project, BranchName branch) {

    public TicketWorktree {
        Assert.notNull("project", project);
        Assert.notNull("branch", branch);
    }

    /** Where the project keeps it, if its root has a name to keep worktrees beside. */
    public Optional<WorkingDirectory> path() {
        return project.ticketWorktree(branch);
    }

    public static TicketWorktreeBuilder builder() {
        return new TicketWorktreeBuilder();
    }

    public static final class TicketWorktreeBuilder {
        private Project project;
        private BranchName branch;

        private TicketWorktreeBuilder() {}

        public TicketWorktreeBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public TicketWorktreeBuilder branch(BranchName branch) {
            this.branch = branch;
            return this;
        }

        public TicketWorktree build() {
            return new TicketWorktree(project, branch);
        }
    }
}

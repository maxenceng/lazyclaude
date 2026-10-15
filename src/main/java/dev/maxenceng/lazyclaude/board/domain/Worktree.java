package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A checkout of the project's repository: where it is, and what it has checked out. */
public record Worktree(WorktreePath path, WorktreeHead head) {

    public Worktree {
        Assert.notNull("worktree path", path);
        Assert.notNull("head", head);
    }

    public static WorktreeBuilder builder() {
        return new WorktreeBuilder();
    }

    public static final class WorktreeBuilder {
        private WorktreePath path;
        private WorktreeHead head;

        private WorktreeBuilder() {}

        public WorktreeBuilder path(WorktreePath path) {
            this.path = path;
            return this;
        }

        public WorktreeBuilder head(WorktreeHead head) {
            this.head = head;
            return this;
        }

        public Worktree build() {
            return new Worktree(path, head);
        }
    }
}

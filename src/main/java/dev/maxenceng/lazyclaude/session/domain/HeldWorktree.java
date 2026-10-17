package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** A ticket worktree that exists: where it is, and the state that decides whether it may be removed. */
public record HeldWorktree(WorkingDirectory path, WorktreeState state) {

    public HeldWorktree {
        Assert.notNull("path", path);
        Assert.notNull("state", state);
    }

    public static HeldWorktreeBuilder builder() {
        return new HeldWorktreeBuilder();
    }

    public static final class HeldWorktreeBuilder {
        private WorkingDirectory path;
        private WorktreeState state;

        private HeldWorktreeBuilder() {}

        public HeldWorktreeBuilder path(WorkingDirectory path) {
            this.path = path;
            return this;
        }

        public HeldWorktreeBuilder state(WorktreeState state) {
            this.state = state;
            return this;
        }

        public HeldWorktree build() {
            return new HeldWorktree(path, state);
        }
    }
}

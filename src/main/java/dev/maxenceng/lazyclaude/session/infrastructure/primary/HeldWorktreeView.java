package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.session.domain.HeldWorktree;

/** A ticket worktree that exists: where it is, what stands in it, and whether that leaves it clear to remove. */
public record HeldWorktreeView(Path path, WorktreeStateView state, boolean clear) implements WorktreeView {

    static HeldWorktreeView from(HeldWorktree held) {
        return builder()
                .path(held.path().path())
                .state(WorktreeStateView.from(held.state()))
                .clear(held.state().clear())
                .build();
    }

    public static HeldWorktreeViewBuilder builder() {
        return new HeldWorktreeViewBuilder();
    }

    public static final class HeldWorktreeViewBuilder {
        private Path path;
        private WorktreeStateView state;
        private boolean clear;

        private HeldWorktreeViewBuilder() {}

        public HeldWorktreeViewBuilder path(Path path) {
            this.path = path;
            return this;
        }

        public HeldWorktreeViewBuilder state(WorktreeStateView state) {
            this.state = state;
            return this;
        }

        public HeldWorktreeViewBuilder clear(boolean clear) {
            this.clear = clear;
            return this;
        }

        public HeldWorktreeView build() {
            return new HeldWorktreeView(path, state, clear);
        }
    }
}

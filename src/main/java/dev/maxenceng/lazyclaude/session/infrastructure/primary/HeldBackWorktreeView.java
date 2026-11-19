package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** A ticket worktree that is not removed: what stands in it, or that no merged pull request says what the merge covered, and the line saying it is held back. */
public record HeldBackWorktreeView(Path shown, WorktreeStateView state, Reason held) implements WorktreeShownView {

    public static HeldBackWorktreeViewBuilder builder() {
        return new HeldBackWorktreeViewBuilder();
    }

    public static final class HeldBackWorktreeViewBuilder {
        private Path shown;
        private WorktreeStateView state;
        private Reason held;

        private HeldBackWorktreeViewBuilder() {}

        public HeldBackWorktreeViewBuilder shown(Path shown) {
            this.shown = shown;
            return this;
        }

        public HeldBackWorktreeViewBuilder state(WorktreeStateView state) {
            this.state = state;
            return this;
        }

        public HeldBackWorktreeViewBuilder held(Reason held) {
            this.held = held;
            return this;
        }

        public HeldBackWorktreeView build() {
            return new HeldBackWorktreeView(shown, state, held);
        }
    }
}

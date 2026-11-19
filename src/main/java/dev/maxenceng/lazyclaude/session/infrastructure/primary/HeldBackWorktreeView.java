package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** A ticket worktree that is not removed: what stands in it, or that no merged pull request says what the merge covered, and the line saying so. */
public record HeldBackWorktreeView(Path shown, WorktreeStateView state, Reason refusal) implements WorktreeShownView {

    public static HeldBackWorktreeViewBuilder builder() {
        return new HeldBackWorktreeViewBuilder();
    }

    public static final class HeldBackWorktreeViewBuilder {
        private Path shown;
        private WorktreeStateView state;
        private Reason refusal;

        private HeldBackWorktreeViewBuilder() {}

        public HeldBackWorktreeViewBuilder shown(Path shown) {
            this.shown = shown;
            return this;
        }

        public HeldBackWorktreeViewBuilder state(WorktreeStateView state) {
            this.state = state;
            return this;
        }

        public HeldBackWorktreeViewBuilder refusal(Reason refusal) {
            this.refusal = refusal;
            return this;
        }

        public HeldBackWorktreeView build() {
            return new HeldBackWorktreeView(shown, state, refusal);
        }
    }
}

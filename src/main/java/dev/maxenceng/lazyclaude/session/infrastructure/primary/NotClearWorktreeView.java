package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** A ticket worktree that is not clear, so not offered for removal: what stands in it, or that no merged pull request says what the merge covered, and the line saying it is held back. */
public record NotClearWorktreeView(Path shown, WorktreeStateView state, Reason held) implements WorktreeShownView {

    public static NotClearWorktreeViewBuilder builder() {
        return new NotClearWorktreeViewBuilder();
    }

    public static final class NotClearWorktreeViewBuilder {
        private Path shown;
        private WorktreeStateView state;
        private Reason held;

        private NotClearWorktreeViewBuilder() {}

        public NotClearWorktreeViewBuilder shown(Path shown) {
            this.shown = shown;
            return this;
        }

        public NotClearWorktreeViewBuilder state(WorktreeStateView state) {
            this.state = state;
            return this;
        }

        public NotClearWorktreeViewBuilder held(Reason held) {
            this.held = held;
            return this;
        }

        public NotClearWorktreeView build() {
            return new NotClearWorktreeView(shown, state, held);
        }
    }
}

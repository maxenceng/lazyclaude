package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** Git could not say whether the ticket has a worktree or what stands in it: why, and the line saying it is held back. */
public record UnreadWorktreeView(Reason reason, Reason held) implements WorktreeShownView {

    public static UnreadWorktreeViewBuilder builder() {
        return new UnreadWorktreeViewBuilder();
    }

    public static final class UnreadWorktreeViewBuilder {
        private Reason reason;
        private Reason held;

        private UnreadWorktreeViewBuilder() {}

        public UnreadWorktreeViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnreadWorktreeViewBuilder held(Reason held) {
            this.held = held;
            return this;
        }

        public UnreadWorktreeView build() {
            return new UnreadWorktreeView(reason, held);
        }
    }
}

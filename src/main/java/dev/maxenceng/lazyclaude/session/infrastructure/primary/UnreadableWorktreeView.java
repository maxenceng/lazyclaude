package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** Git could not say whether the ticket has a worktree or what stands in it, and why. */
public record UnreadableWorktreeView(Reason reason) implements WorktreeView {

    /** Why nothing is removed, in the words every frontend shows. */
    public Reason refusal() {
        return WorktreeWording.failure(reason);
    }
}

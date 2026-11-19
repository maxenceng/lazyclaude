package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** Git could not say whether the ticket has a worktree or what stands in it, and why. */
public record UnreadableWorktreeView(Reason reason) implements WorktreeView {

    /** Why git could not read the worktree, as the ticket detail says it. */
    public Reason summary() {
        return WorktreeWording.unreadable(reason);
    }

    /** The line saying the worktree is held back, with why git could not read it. */
    public Reason heldBack() {
        return WorktreeWording.heldBackUnreadable(reason);
    }

    /** Why nothing is removed, in the words every frontend shows. */
    public Reason refusal() {
        return WorktreeWording.failure(reason);
    }
}

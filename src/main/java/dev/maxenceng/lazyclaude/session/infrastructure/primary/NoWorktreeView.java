package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** The ticket has no worktree at the path the project keeps them at: never created, or already removed. */
public record NoWorktreeView() implements WorktreeView, WorktreeShownView {

    private static final Reason NONE = new Reason("the ticket has no worktree");

    /** Why nothing is removed, in the words every frontend shows. */
    public Reason refusal() {
        return WorktreeWording.failure(NONE);
    }
}

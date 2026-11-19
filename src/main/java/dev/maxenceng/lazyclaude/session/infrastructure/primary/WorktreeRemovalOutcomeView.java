package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** What asking the desktop window's removal of a ticket worktree came to: removed, or not, and the line saying why. */
public sealed interface WorktreeRemovalOutcomeView permits RemovedWorktreeView, NotRemovedWorktreeView {

    static WorktreeRemovalOutcomeView from(WorktreeRemovalView removal) {
        return switch (removal) {
            case RemovedWorktreeView removed -> removed;
            case RefusedRemovalView refused -> new NotRemovedWorktreeView(refused.state().refusal());
            case FailedRemovalView failed -> new NotRemovedWorktreeView(failed.refusal());
        };
    }
}

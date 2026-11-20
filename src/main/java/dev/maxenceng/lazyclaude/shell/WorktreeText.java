package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.FailedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HeldWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeStateView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeView;

/**
 * How the shell words a ticket worktree: the row of the ticket detail, the question on the status line and the reasons it is
 * refused. The detail and the refusal read the same state and count the same things.
 */
final class WorktreeText {

    private static final String PROMPT = "remove worktree %s? y/n";
    private static final String NO_STATE = "";

    private WorktreeText() {}

    /** What follows {@code worktree} in the ticket detail: the path from the project root and the state; none when there is no worktree. */
    static Optional<WorktreeFacts> detail(Path root, WorktreeView view) {
        return switch (view) {
            case HeldWorktreeView held ->
                Optional.of(new WorktreeFacts(held.shown(root).toString(), held.state().summary().value()));
            case UnreadableWorktreeView unreadable ->
                Optional.of(new WorktreeFacts(unreadable.summary().value(), NO_STATE));
            case NoWorktreeView _ -> Optional.empty();
        };
    }

    static String prompt(Path root, HeldWorktreeView worktree) {
        return PROMPT.formatted(worktree.shown(root));
    }

    /** Why the worktree is not removed, for the key bar. */
    static String refusal(WorktreeView view) {
        return switch (view) {
            case HeldWorktreeView held -> refusal(held.state());
            case UnreadableWorktreeView unreadable -> unreadable.refusal().value();
            case NoWorktreeView none -> none.refusal().value();
        };
    }

    static String refusal(WorktreeStateView state) {
        return state.refusal().value();
    }

    static String failure(FailedRemovalView failed) {
        return failed.refusal().value();
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/**
 * A ticket's worktree as the desktop window shows it: where it is from the project's root, what stands in it and why it is not
 * removed, worded as the terminal words it, so the window carries no wording of its own.
 */
public sealed interface WorktreeShownView
        permits NoWorktreeView, ClearWorktreeView, NotClearWorktreeView, UnreadWorktreeView {

    /** What a frontend that has not yet watched the project's sessions is told: nothing can be judged, so nothing is offered. */
    static WorktreeShownView notWatched() {
        UnreadableWorktreeView unread = new UnreadableWorktreeView(WorktreeWording.notWatched());
        return UnreadWorktreeView.builder().reason(unread.reason()).held(unread.heldBack()).build();
    }

    static WorktreeShownView from(WorktreeView view, SessionPayload sessions) {
        return switch (view) {
            case HeldWorktreeView held when held.clear() ->
                new ClearWorktreeView(held.shown(sessions.root()));
            case HeldWorktreeView held ->
                NotClearWorktreeView.builder()
                        .shown(held.shown(sessions.root()))
                        .state(held.state())
                        .held(held.state().heldBack())
                        .build();
            case UnreadableWorktreeView unreadable ->
                UnreadWorktreeView.builder().reason(unreadable.reason()).held(unreadable.heldBack()).build();
            case NoWorktreeView none -> none;
        };
    }
}

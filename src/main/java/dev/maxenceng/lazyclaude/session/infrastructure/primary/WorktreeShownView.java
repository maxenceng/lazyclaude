package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/**
 * A ticket's worktree as the desktop window shows it: where it is from the project's parent, what stands in it and why it is not
 * removed, worded as the terminal words it, so the window carries no wording of its own.
 */
public sealed interface WorktreeShownView
        permits NoWorktreeView, ClearWorktreeView, HeldBackWorktreeView, UnreadWorktreeView {

    static WorktreeShownView from(WorktreeView view, SessionPayload sessions) {
        return switch (view) {
            case HeldWorktreeView held when held.clear() ->
                new ClearWorktreeView(sessions.root().relativize(held.path()));
            case HeldWorktreeView held ->
                HeldBackWorktreeView.builder()
                        .shown(sessions.root().relativize(held.path()))
                        .state(held.state())
                        .refusal(held.state().refusal())
                        .build();
            case UnreadableWorktreeView unreadable ->
                UnreadWorktreeView.builder().reason(unreadable.reason()).refusal(unreadable.refusal()).build();
            case NoWorktreeView none -> none;
        };
    }
}

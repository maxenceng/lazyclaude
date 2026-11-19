package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.NoMergedPullRequest;
import dev.maxenceng.lazyclaude.session.domain.WorktreeContents;
import dev.maxenceng.lazyclaude.session.domain.WorktreeState;
import dev.maxenceng.lazyclaude.session.published.Reason;

/** What stands in a ticket worktree, as the detail shows it and a refusal names it. */
public sealed interface WorktreeStateView permits WorktreeContentsView, NoMergedPullRequestView {

    /** What stands in a worktree in this state, as the ticket detail counts it. */
    default Reason summary() {
        return WorktreeWording.summary(this);
    }

    /** The line saying a worktree in this state is held back, with what stands in it. */
    default Reason heldBack() {
        return WorktreeWording.heldBack(this);
    }

    /** Why a worktree in this state is not removed, in the words every frontend shows. */
    default Reason refusal() {
        return WorktreeWording.refusal(this);
    }

    static WorktreeStateView from(WorktreeState state) {
        return switch (state) {
            case WorktreeContents contents -> WorktreeContentsView.from(contents);
            case NoMergedPullRequest _ -> new NoMergedPullRequestView();
        };
    }
}

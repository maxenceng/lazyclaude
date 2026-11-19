package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.NoMergedPullRequest;
import dev.maxenceng.lazyclaude.session.domain.WorktreeContents;
import dev.maxenceng.lazyclaude.session.domain.WorktreeState;
import dev.maxenceng.lazyclaude.session.published.Reason;

/** What stands in a ticket worktree, as the detail shows it and a refusal names it. */
public sealed interface WorktreeStateView permits WorktreeContentsView, NoMergedPullRequestView {

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

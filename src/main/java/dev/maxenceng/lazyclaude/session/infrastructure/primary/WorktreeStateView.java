package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.NoMergedPullRequest;
import dev.maxenceng.lazyclaude.session.domain.NoSessionsRead;
import dev.maxenceng.lazyclaude.session.domain.WorktreeContents;
import dev.maxenceng.lazyclaude.session.domain.WorktreeState;

/** What stands in a ticket worktree, as the detail shows it and a refusal names it. */
public sealed interface WorktreeStateView permits WorktreeContentsView, NoMergedPullRequestView, NoSessionsReadView {

    static WorktreeStateView from(WorktreeState state) {
        return switch (state) {
            case WorktreeContents contents -> WorktreeContentsView.from(contents);
            case NoMergedPullRequest _ -> new NoMergedPullRequestView();
            case NoSessionsRead _ -> new NoSessionsReadView();
        };
    }
}

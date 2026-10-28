package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.review.published.HeadCommit;
import dev.maxenceng.lazyclaude.review.published.MergedHead;
import dev.maxenceng.lazyclaude.review.published.NoMergedHead;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Judges a ticket worktree against its merged pull request and removes it only when nothing stands in it: no dirty path, no commit
 * beyond the merged head that no remote ref has, no hosted session. The state that refuses is the state shown, and it is read again at
 * the removal, since it may have changed since it was shown.
 */
public record WorktreeManager(WorktreePort worktrees) {

    /** The worktree at the ticket's path, with its state; none when the project keeps no worktrees or git lists none there. */
    public Optional<HeldWorktree> held(TicketWorktree worktree, MergedHead merged, AvailableSessions sessions) {
        return worktree.path()
                .filter(at -> worktrees.isWorktreeAt(worktree.project(), at))
                .map(at -> HeldWorktree.builder().path(at).state(stateOf(at, merged, sessions)).build());
    }

    /**
     * Removes the ticket worktree, keeping its branch; nothing when there is none.
     *
     * @throws WorktreeNotClearException when something stands in it
     */
    public void remove(TicketWorktree worktree, MergedHead merged, AvailableSessions sessions) {
        held(worktree, merged, sessions).ifPresent(held -> {
            if (!held.state().clear()) {
                throw new WorktreeNotClearException(held.state());
            }
            worktrees.remove(worktree.project(), held.path());
        });
    }

    private WorktreeState stateOf(WorkingDirectory at, MergedHead merged, AvailableSessions sessions) {
        return switch (merged) {
            case NoMergedHead _ -> new NoMergedPullRequest();
            case HeadCommit head ->
                WorktreeContents.builder()
                        .dirtyPaths(worktrees.dirtyPaths(at))
                        .commitsBeyondMerge(worktrees.commitsBeyond(at, head))
                        .sessions(sessions.namesIn(at))
                        .build();
        };
    }
}

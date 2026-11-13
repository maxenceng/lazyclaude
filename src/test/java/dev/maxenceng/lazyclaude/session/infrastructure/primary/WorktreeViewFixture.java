package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.dirtyPath;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;

import java.util.List;

public final class WorktreeViewFixture {

    private WorktreeViewFixture() {}

    public static WorktreeContentsView.WorktreeContentsViewBuilder clearWorktreeContentsViewBuilder() {
        return WorktreeContentsView.builder().dirtyPaths(List.of()).commitsBeyondMerge(0).sessions(List.of());
    }

    public static WorktreeContentsView dirtyWorktreeContentsView() {
        return clearWorktreeContentsViewBuilder().dirtyPaths(List.of(dirtyPath())).build();
    }

    public static WorktreeContentsView aheadWorktreeContentsView() {
        return clearWorktreeContentsViewBuilder().commitsBeyondMerge(1).build();
    }

    public static WorktreeContentsView occupiedWorktreeContentsView() {
        return clearWorktreeContentsViewBuilder().sessions(List.of(sessionName())).build();
    }

    public static HeldWorktreeView heldWorktreeView(WorktreeStateView state, boolean clear) {
        return HeldWorktreeView.builder().path(worktreeDirectory().path()).state(state).clear(clear).build();
    }

    public static HeldWorktreeView clearHeldWorktreeView() {
        return heldWorktreeView(clearWorktreeContentsViewBuilder().build(), true);
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.CommitIdFixture.commitId;
import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.dirtyPath;
import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.secondDirtyPath;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;

import java.util.List;

public final class WorktreeStateFixture {

    private WorktreeStateFixture() {}

    public static WorktreeContents.WorktreeContentsBuilder clearWorktreeContentsBuilder() {
        return WorktreeContents.builder();
    }

    public static WorktreeContents clearWorktreeContents() {
        return clearWorktreeContentsBuilder().build();
    }

    public static WorktreeContents dirtyWorktreeContents() {
        return clearWorktreeContentsBuilder()
                .dirtyPaths(List.of(dirtyPath(), secondDirtyPath()))
                .build();
    }

    public static WorktreeContents aheadWorktreeContents() {
        return clearWorktreeContentsBuilder()
                .commitsBeyondMerge(List.of(commitId()))
                .build();
    }

    public static WorktreeContents occupiedWorktreeContents() {
        return clearWorktreeContentsBuilder().sessions(List.of(sessionName())).build();
    }

    public static HeldWorktree heldWorktree(WorktreeState state) {
        return HeldWorktree.builder().path(worktreeDirectory()).state(state).build();
    }
}

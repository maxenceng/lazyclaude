package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.aheadWorktreeContents;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.clearWorktreeContents;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.dirtyWorktreeContents;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.occupiedWorktreeContents;
import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.dirtyPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class WorktreeStateTest {

    @Test
    void shouldBeClearWhenNothingStandsInTheWorktree() {
        assertThat(clearWorktreeContents().clear()).isTrue();
    }

    @Test
    void shouldNotBeClearWithOneDirtyPath() {
        assertThat(WorktreeContents.builder().dirtyPaths(List.of(dirtyPath())).build().clear())
                .as("one path is enough: the rule is not a threshold")
                .isFalse();
    }

    @Test
    void shouldNotBeClearWithSeveralDirtyPaths() {
        assertThat(dirtyWorktreeContents().clear()).isFalse();
    }

    @Test
    void shouldNotBeClearWithACommitBeyondTheMerge() {
        assertThat(aheadWorktreeContents().clear()).isFalse();
    }

    @Test
    void shouldNotBeClearWithALiveSessionInIt() {
        assertThat(occupiedWorktreeContents().clear()).isFalse();
    }

    @Test
    void shouldNeverBeClearWithoutAMergedPullRequest() {
        assertThat(new NoMergedPullRequest().clear())
                .as("what the merge covered cannot be told, so no state of the worktree can be judged")
                .isFalse();
    }
}

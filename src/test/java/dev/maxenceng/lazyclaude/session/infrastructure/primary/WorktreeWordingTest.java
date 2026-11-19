package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.aheadWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.occupiedWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Reason;

class WorktreeWordingTest {

    @Test
    void shouldNameTheDirtyPathsOfAWorktreeItRefuses() {
        assertThat(WorktreeWording.refusal(dirtyWorktreeContentsView()))
                .isEqualTo(new Reason("cannot remove: 1 dirty path — src/Foo.java"));
    }

    @Test
    void shouldSayNoRemoteHasTheCommitsBeyondTheMerge() {
        assertThat(WorktreeWording.refusal(aheadWorktreeContentsView()))
                .isEqualTo(new Reason("cannot remove: 1 commit beyond the merge that no remote has"));
    }

    @Test
    void shouldNameTheLiveSessionsInTheWorktree() {
        assertThat(WorktreeWording.refusal(occupiedWorktreeContentsView()))
                .isEqualTo(new Reason("cannot remove: live session \"lazyclaude-b6\" is in the worktree"));
    }

    @Test
    void shouldSayWhatTheMergeCoveredCannotBeToldWhenNoMergedPullRequestIsKnown() {
        assertThat(WorktreeWording.refusal(new NoMergedPullRequestView()))
                .isEqualTo(new Reason(
                        "cannot remove: cannot tell what the merge covered (no merged pull request found for the branch)"));
    }

    @Test
    void shouldGiveGitsReasonAsWhyItCannotRemove() {
        assertThat(WorktreeWording.failure(reason())).isEqualTo(new Reason("cannot remove: no registry and no claude"));
    }
}

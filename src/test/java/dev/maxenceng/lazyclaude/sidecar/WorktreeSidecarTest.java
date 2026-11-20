package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.worktreePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.clearHeldWorktreeView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.heldWorktreeView;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.answerTo;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.clearWorktree;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.endingTheDoneSessionOf;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.havingReadMerged;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.served;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.servedTo;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.ticketDoneEnded;
import static dev.maxenceng.lazyclaude.sidecar.WorktreeSidecarFixture.watchingTheProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.published.NoMergedHead;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.FailedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RefusedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RemovedWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableWorktreeView;

@ExtendWith(MockitoExtension.class)
class WorktreeSidecarTest {

    @Mock
    private SessionQuery sessions;

    @Mock
    private TicketWorktreeCommand removing;

    @Mock
    private ReviewQuery review;

    @Test
    void reads_a_worktree_as_unreadable_before_any_project_is_watched() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);

        assertThat(answerTo(methods, "session/worktreeOf"))
                .isEqualTo(
                        "{\"kind\":\"unreadWorktree\",\"reason\":\"the project's sessions are not watched yet\",\"held\":\"cannot remove: unreadable (the project's sessions are not watched yet)\"}");
        verify(sessions, never()).worktreeOf(any());
    }

    @Test
    void reads_a_worktree_with_the_merged_head_review_last_read_for_the_branch() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        havingReadMerged(methods, review, branchName().value());
        when(sessions.worktreeOf(worktreePayload(headCommit()))).thenReturn(clearHeldWorktreeView());

        assertThat(answerTo(methods, "session/worktreeOf")).isEqualTo(clearWorktree());
    }

    @Test
    void reads_a_branch_no_merged_pull_request_has_as_having_no_merged_head() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        havingReadMerged(methods, review, "another-branch");
        when(sessions.worktreeOf(worktreePayload(new NoMergedHead()))).thenReturn(clearHeldWorktreeView());

        assertThat(answerTo(methods, "session/worktreeOf")).isEqualTo(clearWorktree());
    }

    @Test
    void reads_a_branch_before_review_has_read_any_merged_pull_request_as_having_no_merged_head() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        when(sessions.worktreeOf(worktreePayload(new NoMergedHead()))).thenReturn(new NoWorktreeView());

        assertThat(answerTo(methods, "session/worktreeOf")).isEqualTo("{\"kind\":\"noWorktree\"}");
    }

    @Test
    void reads_a_worktree_with_something_in_it_with_what_stands_there_and_why_it_is_not_removed() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        when(sessions.worktreeOf(worktreePayload(new NoMergedHead())))
                .thenReturn(heldWorktreeView(dirtyWorktreeContentsView(), false));

        assertThat(answerTo(methods, "session/worktreeOf"))
                .isEqualTo(
                        "{\"kind\":\"notClearWorktree\",\"shown\":\"../app.worktrees/PROJ-1-ticket\",\"state\":{\"kind\":\"worktreeContents\",\"dirtyPaths\":[\"src/Foo.java\"],\"commitsBeyondMerge\":0,\"sessions\":[]},\"held\":\"cannot remove: 1 dirty path\"}");
    }

    @Test
    void reads_a_worktree_git_could_not_read_with_why() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        when(sessions.worktreeOf(worktreePayload(new NoMergedHead()))).thenReturn(new UnreadableWorktreeView(reason()));

        assertThat(answerTo(methods, "session/worktreeOf"))
                .isEqualTo(
                        "{\"kind\":\"unreadWorktree\",\"reason\":\"no registry and no claude\",\"held\":\"cannot remove: unreadable (no registry and no claude)\"}");
    }

    @Test
    void refuses_a_branch_that_is_blank_as_invalid_params() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);

        assertThatThrownBy(() -> methods.get("session/worktreeOf").call(SidecarJson.parse("{\"branch\":\" \"}")))
                .isInstanceOf(JsonRpcFaultException.class)
                .hasMessageContaining("invalid params");
    }

    @Test
    void removes_the_worktree_judged_again_with_the_merged_head_review_last_read() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        havingReadMerged(methods, review, branchName().value());
        when(removing.remove(worktreePayload(headCommit()))).thenReturn(new RemovedWorktreeView());

        assertThat(answerTo(methods, "session/removeWorktree")).isEqualTo("{\"kind\":\"removedWorktree\"}");
    }

    @Test
    void answers_a_refused_removal_as_a_result_with_what_stands_in_the_worktree() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        when(removing.remove(worktreePayload(new NoMergedHead()))).thenReturn(new RefusedRemovalView(dirtyWorktreeContentsView()));

        assertThat(answerTo(methods, "session/removeWorktree"))
                .isEqualTo("{\"kind\":\"notRemovedWorktree\",\"refusal\":\"cannot remove: 1 dirty path — src/Foo.java\"}");
    }

    @Test
    void answers_a_failed_removal_as_a_result_with_gits_reason() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);
        watchingTheProject(methods, sessions);
        when(removing.remove(worktreePayload(new NoMergedHead()))).thenReturn(new FailedRemovalView(reason()));

        assertThat(answerTo(methods, "session/removeWorktree"))
                .isEqualTo("{\"kind\":\"notRemovedWorktree\",\"refusal\":\"cannot remove: no registry and no claude\"}");
    }

    @Test
    void answers_a_removal_before_any_project_is_watched_as_not_removed() {
        Map<String, SidecarMethod> methods = served(sessions, removing, review);

        assertThat(answerTo(methods, "session/removeWorktree"))
                .isEqualTo(
                        "{\"kind\":\"notRemovedWorktree\",\"refusal\":\"cannot remove: the project's sessions are not watched yet\"}");
        verify(removing, never()).remove(any());
    }

    @Test
    void pushes_the_ticket_whose_done_session_ended() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doAnswer(endingTheDoneSessionOf(linkedTicketId())).when(sessions).onTicketDoneEnded(any());

        try (SidecarOutput output = new SidecarOutput(out)) {
            servedTo(sessions, removing, review, output);
        }

        assertThat(out.toString(StandardCharsets.UTF_8).lines()).containsExactly(ticketDoneEnded());
    }
}

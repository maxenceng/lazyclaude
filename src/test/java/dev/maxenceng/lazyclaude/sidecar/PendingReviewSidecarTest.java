package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingCommentPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingDeletePayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingEditPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingReplyPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingSubmitPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.refusedWriteView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.writtenView;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.commentParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.deleteParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.editParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.refusedWrite;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.replyParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.submitParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.writtenWithNoPending;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;

@ExtendWith(MockitoExtension.class)
class PendingReviewSidecarTest {

    @Mock
    private PendingReviewCommand writing;

    @Test
    void answers_what_github_took_of_a_comment() {
        when(writing.comment(pendingCommentPayload())).thenReturn(writtenView(noPendingReviewView()));

        assertThat(linesWritten(writing, request(1, "review/comment", commentParams())))
                .containsExactly(ready(), resultOf(1, writtenWithNoPending()));
    }

    @Test
    void answers_a_comment_github_refused_as_a_result() {
        when(writing.comment(pendingCommentPayload())).thenReturn(refusedWriteView());

        assertThat(linesWritten(writing, request(1, "review/comment", commentParams())))
                .as("a refusal is what the write came to, which the frontend shows, not a failure of the call")
                .containsExactly(ready(), resultOf(1, refusedWrite()));
    }

    @Test
    void answers_what_github_took_of_a_reply() {
        when(writing.reply(pendingReplyPayload())).thenReturn(writtenView(noPendingReviewView()));

        assertThat(linesWritten(writing, request(1, "review/reply", replyParams())))
                .containsExactly(ready(), resultOf(1, writtenWithNoPending()));
    }

    @Test
    void answers_what_github_took_of_an_edit() {
        when(writing.edit(pendingEditPayload())).thenReturn(writtenView(noPendingReviewView()));

        assertThat(linesWritten(writing, request(1, "review/edit", editParams())))
                .containsExactly(ready(), resultOf(1, writtenWithNoPending()));
    }

    @Test
    void answers_what_github_took_of_a_deletion() {
        when(writing.delete(pendingDeletePayload())).thenReturn(writtenView(noPendingReviewView()));

        assertThat(linesWritten(writing, request(1, "review/delete", deleteParams())))
                .containsExactly(ready(), resultOf(1, writtenWithNoPending()));
    }

    @Test
    void answers_a_submit_github_refused_as_a_result() {
        when(writing.submit(pendingSubmitPayload())).thenReturn(refusedWriteView());

        assertThat(linesWritten(writing, request(1, "review/submit", submitParams())))
                .containsExactly(ready(), resultOf(1, refusedWrite()));
    }
}

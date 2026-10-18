package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReviewId;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.reviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.openPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.refusedWriteView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.writtenView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQueryFixture.pendingReviewCommand;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.editedCommentBody;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.pendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.SummaryFixture.summary;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.domain.PendingReviewPort;

@ExtendWith(MockitoExtension.class)
class PendingReviewCommandTest {

    private static final PullRequestPayload OF_THE_PULL_REQUEST = new PullRequestPayload(project().root(), pullRequestNumber());

    @Mock
    private PendingReviewPort pending;

    private PendingReviewCommand command;

    @BeforeEach
    void createCommand() {
        command = pendingReviewCommand(pending);
    }

    @Test
    void shouldWriteACommentOnTheDiffAndViewThePendingReviewReadAfterIt() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());
        PendingCommentPayload payload = new PendingCommentPayload(
                project().root(), pullRequestNumber(), diffPath(), rightOn(3), commentBody());

        assertThat(command.comment(payload)).isEqualTo(writtenView(openPendingView()));
        verify(pending).addThread(project(), pendingReviewId(), onDiffLine(), commentBody());
    }

    @Test
    void shouldViewWhyAWriteWasRefusedInsteadOfFailing() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());
        doThrow(pullRequestsUnavailable()).when(pending).addThread(any(), any(), any(), any());
        PendingCommentPayload payload = new PendingCommentPayload(
                project().root(), pullRequestNumber(), diffPath(), rightOn(3), commentBody());

        assertThat(command.comment(payload)).isEqualTo(refusedWriteView());
    }

    @Test
    void shouldWriteAReplyToAThread() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        command.reply(new PendingReplyPayload(project().root(), pullRequestNumber(), threadId(), commentBody()));

        verify(pending).reply(project(), pendingReviewId(), threadId(), commentBody());
    }

    @Test
    void shouldEditAPendingComment() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        assertThat(command.edit(new PendingEditPayload(project().root(), pullRequestNumber(), pendingCommentId(), editedCommentBody())))
                .isEqualTo(writtenView(openPendingView()));
        verify(pending).edit(project(), pendingCommentId(), editedCommentBody());
    }

    @Test
    void shouldDeleteAPendingComment() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        command.delete(new PendingDeletePayload(project().root(), pullRequestNumber(), pendingCommentId()));

        verify(pending).delete(project(), pendingCommentId());
    }

    @Test
    void shouldSetTheSummary() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        command.summarise(new PendingSummaryPayload(project().root(), pullRequestNumber(), summary()));

        verify(pending).summarise(project(), pendingReviewId(), summary());
    }

    @Test
    void shouldSubmitThePendingReview() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        command.submit(OF_THE_PULL_REQUEST);

        verify(pending).submit(project(), pendingReviewId());
    }
}

package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.commentOn;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiff;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onNoDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReviewOf;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.replyToThread;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.reviewedPullRequestWith;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManagerFixture.unavailableDiff;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReview;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReviewId;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pullRequestNodeId;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.reviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.unreviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewManagerFixture.refusedWrite;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewManagerFixture.unavailablePendingReview;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.blankCommentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.editedCommentBody;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.pendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.SummaryFixture.summary;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.Reason;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingReviewManagerTest {

    @Mock
    private PendingReviewPort reviews;

    private PendingReviewManager manager;

    @BeforeEach
    void createManager() {
        manager = new PendingReviewManager(reviews);
    }

    @Test
    void shouldBeLoadingThePendingReviewBeforeTheReadAnswers() {
        assertThat(manager.loading()).isEqualTo(new LoadingPendingReview());
    }

    @Test
    void shouldReadThePullRequestWithItsPendingReview() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        assertThat(manager.read(project(), pullRequestNumber())).isEqualTo(new ReadPendingReview(reviewedPullRequest()));
    }

    @Test
    void shouldAnswerWhyThePendingReviewCannotBeReadInsteadOfFailing() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenThrow(pullRequestsUnavailable());

        assertThat(manager.read(project(), pullRequestNumber())).isEqualTo(unavailablePendingReview());
    }

    @Test
    void shouldCreateThePendingReviewWithTheFirstSavedCommentThenAddTheCommentToIt() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(unreviewedPullRequest(), reviewedPullRequest());
        when(reviews.create(project(), pullRequestNodeId())).thenReturn(pendingReviewId());

        PendingReviewWrite written = manager.comment(project(), pullRequestNumber(), onDiffLine(), commentBody());

        InOrder order = inOrder(reviews);
        order.verify(reviews).create(project(), pullRequestNodeId());
        order.verify(reviews).addThread(project(), pendingReviewId(), onDiffLine(), commentBody());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
    }

    @Test
    void shouldAddALaterCommentToTheExistingPendingReviewWithoutStartingAnother() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        PendingReviewWrite written = manager.comment(project(), pullRequestNumber(), onDiffLine(), commentBody());

        verify(reviews).addThread(project(), pendingReview().id(), onDiffLine(), commentBody());
        verify(reviews, never()).create(any(), any());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
    }

    @Test
    void shouldWriteNothingForAnEmptyNewComment() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        PendingReviewWrite written = manager.comment(project(), pullRequestNumber(), onDiffLine(), blankCommentBody());

        verify(reviews, never()).addThread(any(), any(), any(), any());
        verify(reviews, never()).create(any(), any());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
    }

    @Test
    void shouldRefuseWithTheReasonWhenGitHubDoesNotTakeTheComment() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());
        doThrow(pullRequestsUnavailable()).when(reviews).addThread(any(), any(), any(), any());

        assertThat(manager.comment(project(), pullRequestNumber(), onDiffLine(), commentBody())).isEqualTo(refusedWrite());
    }

    @Test
    void shouldRefuseWithTheReasonWhenTheReviewCannotBeStartedForTheFirstComment() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(unreviewedPullRequest());
        when(reviews.create(project(), pullRequestNodeId())).thenThrow(pullRequestsUnavailable());

        assertThat(manager.comment(project(), pullRequestNumber(), onDiffLine(), commentBody())).isEqualTo(refusedWrite());
        verify(reviews, never()).addThread(any(), any(), any(), any());
    }

    @Test
    void shouldAnswerWrittenWithThePendingReviewUnreadableWhenOnlyTheReadAfterTheWriteFails() {
        when(reviews.pullRequest(project(), pullRequestNumber()))
                .thenReturn(reviewedPullRequest())
                .thenThrow(pullRequestsUnavailable());

        assertThat(manager.comment(project(), pullRequestNumber(), onDiffLine(), commentBody()))
                .as("the comment is on GitHub, so a retry would write it twice")
                .isEqualTo(new WrittenReview(unavailablePendingReview()));
    }

    @Test
    void shouldAddAReplyToTheThreadInTheExistingPendingReview() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        PendingReviewWrite written = manager.reply(project(), pullRequestNumber(), threadId(), commentBody());

        verify(reviews).reply(project(), pendingReview().id(), threadId(), commentBody());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
    }

    @Test
    void shouldStartThePendingReviewWithAReplyWhenItIsTheFirstComment() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(unreviewedPullRequest(), reviewedPullRequest());
        when(reviews.create(project(), pullRequestNodeId())).thenReturn(pendingReviewId());

        manager.reply(project(), pullRequestNumber(), threadId(), commentBody());

        InOrder order = inOrder(reviews);
        order.verify(reviews).create(project(), pullRequestNodeId());
        order.verify(reviews).reply(project(), pendingReviewId(), threadId(), commentBody());
    }

    @Test
    void shouldReplaceWhatAPendingCommentSays() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        PendingReviewWrite written = manager.edit(project(), pullRequestNumber(), pendingCommentId(), editedCommentBody());

        verify(reviews).edit(project(), pendingCommentId(), editedCommentBody());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
    }

    @Test
    void shouldDeleteAPendingCommentSavedEmpty() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        manager.edit(project(), pullRequestNumber(), pendingCommentId(), blankCommentBody());

        verify(reviews).delete(project(), pendingCommentId());
        verify(reviews, never()).edit(any(), any(), any());
    }

    @Test
    void shouldDeleteAPendingComment() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        PendingReviewWrite written = manager.delete(project(), pullRequestNumber(), pendingCommentId());

        verify(reviews).delete(project(), pendingCommentId());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
    }

    @Test
    void shouldRefuseWithTheReasonWhenGitHubDoesNotDeleteTheComment() {
        doThrow(pullRequestsUnavailable()).when(reviews).delete(any(), any());

        assertThat(manager.delete(project(), pullRequestNumber(), pendingCommentId())).isEqualTo(refusedWrite());
    }

    @Test
    void shouldSetTheSummaryOfTheExistingPendingReview() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        PendingReviewWrite written = manager.summarise(project(), pullRequestNumber(), summary());

        verify(reviews).summarise(project(), pendingReviewId(), summary());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
    }

    @Test
    void shouldStartThePendingReviewWithTheSummaryWhenThereIsNoCommentYet() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(unreviewedPullRequest(), reviewedPullRequest());
        when(reviews.create(project(), pullRequestNodeId())).thenReturn(pendingReviewId());

        manager.summarise(project(), pullRequestNumber(), summary());

        InOrder order = inOrder(reviews);
        order.verify(reviews).create(project(), pullRequestNodeId());
        order.verify(reviews).summarise(project(), pendingReviewId(), summary());
    }

    @Test
    void shouldSubmitThePendingReviewAndAnswerThePullRequestAsGitHubHasItAfterwards() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest(), unreviewedPullRequest());

        PendingReviewWrite written = manager.submit(project(), pullRequestNumber());

        verify(reviews).submit(project(), pendingReviewId());
        assertThat(written).isEqualTo(new WrittenReview(new ReadPendingReview(unreviewedPullRequest())));
    }

    @Test
    void shouldRefuseWithTheReasonWhenGitHubRefusesTheSubmit() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());
        doThrow(pullRequestsUnavailable()).when(reviews).submit(any(), any());

        assertThat(manager.submit(project(), pullRequestNumber())).isEqualTo(refusedWrite());
    }

    @Test
    void shouldRefuseASubmitWhenThePullRequestHasNoPendingReviewToSend() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(unreviewedPullRequest());

        assertThat(manager.submit(project(), pullRequestNumber()))
                .isEqualTo(new RefusedWrite(new Reason("the pull request has no pending review to submit")));
        verify(reviews, never()).submit(any(), any());
    }

    @Test
    void shouldWriteNothingForAnEmptyReply() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        manager.reply(project(), pullRequestNumber(), threadId(), blankCommentBody());

        verify(reviews, never()).reply(any(), any(), any(), any());
    }

    @Test
    void shouldPlaceThePendingCommentsOnTheDiffOnceBothAreRead() {
        PendingComment onTheAddedLine = commentOn(diffPath(), rightOn(3));

        assertThat(manager.pendingInDiff(
                        new ReadDiff(pullRequestDiffOf(modifiedFile())),
                        new ReadPendingReview(reviewedPullRequestWith(pendingReviewOf(onTheAddedLine)))))
                .isEqualTo(new PendingCommentsInDiff(List.of(onDiffLine(onTheAddedLine, 2))));
    }

    @Test
    void shouldPutEveryPendingCommentOnNoLineUntilTheDiffIsRead() {
        PendingComment onTheAddedLine = commentOn(diffPath(), rightOn(3));

        assertThat(manager.pendingInDiff(
                        unavailableDiff(), new ReadPendingReview(reviewedPullRequestWith(pendingReviewOf(onTheAddedLine, replyToThread())))))
                .as("the Comments panel lists the pending comments as soon as they are read, whatever became of the diff")
                .isEqualTo(new PendingCommentsInDiff(List.of(onNoDiffLine(onTheAddedLine), onNoDiffLine(replyToThread()))));
    }

    @Test
    void shouldHoldNoPendingCommentUntilThePendingReviewIsReadOrWhenThereIsNone() {
        ReadDiff diff = new ReadDiff(pullRequestDiffOf(modifiedFile()));

        assertThat(manager.pendingInDiff(diff, unavailablePendingReview())).isEqualTo(new PendingCommentsInDiff(List.of()));
        assertThat(manager.pendingInDiff(diff, new LoadingPendingReview())).isEqualTo(new PendingCommentsInDiff(List.of()));
        assertThat(manager.pendingInDiff(diff, new ReadPendingReview(unreviewedPullRequest())))
                .isEqualTo(new PendingCommentsInDiff(List.of()));
    }

    @Test
    void shouldAnchorACommentOnTheLinesOfTheReadDiff() {
        assertThat(manager.anchor(new ReadDiff(pullRequestDiffOf(modifiedFile())), new DiffLineIndex(2), new DiffLineIndex(3)))
                .isEqualTo(onDiff(diffPath(), rightRange(3, 4)));
    }

    @Test
    void shouldRefuseToAnchorACommentWhileThereIsNoDiffToComment() {
        assertThatThrownBy(() -> manager.anchor(unavailableDiff(), new DiffLineIndex(0), new DiffLineIndex(0)))
                .isInstanceOf(DiffLineMissingException.class);
    }
}

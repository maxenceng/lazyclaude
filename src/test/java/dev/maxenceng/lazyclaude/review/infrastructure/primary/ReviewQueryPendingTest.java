package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.movedReviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.reviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.unreviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.commentOnDiffLineView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.loadingPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.movedOpenPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.onDiffAnchorView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.openPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingOnLineView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingOnNoLineView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.replyView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.unavailablePendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQueryFixture.reviewQuery;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.domain.PendingReviewPort;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.Reason;

@ExtendWith(MockitoExtension.class)
class ReviewQueryPendingTest {

    private static final PullRequestPayload OF_THE_PULL_REQUEST = new PullRequestPayload(project().root(), pullRequestNumber());

    @Mock
    private PendingReviewPort pending;

    private ReviewQuery query;

    @BeforeEach
    void createQuery() {
        query = reviewQuery(pending);
    }

    @Test
    void shouldViewLoadingBeforeTheReadAnswers() {
        assertThat(query.loadingPending()).isEqualTo(loadingPendingView());
    }

    @Test
    void shouldViewThePendingReviewWithItsSummaryItsCommentsAndNoMovedHead() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());

        assertThat(query.pendingOf(OF_THE_PULL_REQUEST)).isEqualTo(openPendingView());
    }

    @Test
    void shouldViewAMovedHeadWhenThePullRequestIsNoLongerAtTheCommitTheReviewWasWrittenOn() {
        when(pending.pullRequest(project(), pullRequestNumber()))
                .thenReturn(movedReviewedPullRequest());

        assertThat(query.pendingOf(OF_THE_PULL_REQUEST)).isEqualTo(movedOpenPendingView());
    }

    @Test
    void shouldViewNoPendingReviewWhenThePullRequestHasNone() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenReturn(unreviewedPullRequest());

        assertThat(query.pendingOf(OF_THE_PULL_REQUEST)).isEqualTo(noPendingReviewView());
    }

    @Test
    void shouldViewWhyThePendingReviewCannotBeReadInsteadOfFailing() {
        when(pending.pullRequest(project(), pullRequestNumber())).thenThrow(pullRequestsUnavailable());

        assertThat(query.pendingOf(OF_THE_PULL_REQUEST)).isEqualTo(unavailablePendingView());
    }

    @Test
    void shouldAnchorACommentOnTheLinesOfTheDiffAsAViewOfWhereItSits() {
        assertThat(query.anchorOf(twoFileDiffView(), new DiffLineIndex(2), new DiffLineIndex(2)))
                .isEqualTo(onDiffAnchorView(diffPath(), rightOn(3)));
        assertThat(query.anchorOf(twoFileDiffView(), new DiffLineIndex(2), new DiffLineIndex(3)))
                .isEqualTo(onDiffAnchorView(diffPath(), rightRange(3, 4)));
    }

    @Test
    void shouldRefuseARangeAcrossHunksSayingWhyInsteadOfFailing() {
        assertThat(query.anchorOf(twoFileDiffView(), new DiffLineIndex(3), new DiffLineIndex(4)))
                .isEqualTo(new RefusedAnchorView(new Reason("a range must stay inside one hunk")));
    }

    @Test
    void shouldRefuseToAnchorACommentWhileTheDiffIsNotRead() {
        assertThat(query.anchorOf(loadingDiffView(), new DiffLineIndex(0), new DiffLineIndex(0)))
                .isEqualTo(new RefusedAnchorView(new Reason("that line is no longer in the diff")));
    }

    @Test
    void shouldPlaceThePendingCommentsOnTheDiffLinesTheyShowUnder() {
        assertThat(query.pendingInDiff(twoFileDiffView(), openPendingView()).comments())
                .containsExactly(pendingOnLineView(commentOnDiffLineView(), 2), pendingOnLineView(replyView(), 2));
    }

    @Test
    void shouldPutEveryPendingCommentOnNoLineUntilTheDiffIsRead() {
        assertThat(query.pendingInDiff(loadingDiffView(), openPendingView()).comments())
                .containsExactly(pendingOnNoLineView(commentOnDiffLineView()), pendingOnNoLineView(replyView()));
    }

    @Test
    void shouldHoldNoPendingCommentUntilThePendingReviewIsReadOrWhenThereIsNone() {
        assertThat(query.pendingInDiff(twoFileDiffView(), loadingPendingView()).comments()).isEqualTo(List.of());
        assertThat(query.pendingInDiff(twoFileDiffView(), noPendingReviewView()).comments()).isEqualTo(List.of());
        assertThat(query.pendingInDiff(twoFileDiffView(), unavailablePendingView()).comments()).isEqualTo(List.of());
    }
}

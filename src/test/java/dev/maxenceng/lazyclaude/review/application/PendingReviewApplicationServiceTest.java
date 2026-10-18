package dev.maxenceng.lazyclaude.review.application;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.commentOn;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiff;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReviewId;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.reviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.pendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.SummaryFixture.summary;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.domain.LoadingPendingReview;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewPort;
import dev.maxenceng.lazyclaude.review.domain.ReadDiff;
import dev.maxenceng.lazyclaude.review.domain.ReadPendingReview;
import dev.maxenceng.lazyclaude.review.domain.WrittenReview;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;

@ExtendWith(MockitoExtension.class)
class PendingReviewApplicationServiceTest {

    @Mock
    private PendingReviewPort reviews;

    private PendingReviewApplicationService service;

    @BeforeEach
    void createService() {
        service = new PendingReviewApplicationService(reviews);
    }

    @Test
    void shouldBeLoadingBeforeTheReadAnswers() {
        assertThat(service.loading()).isEqualTo(new LoadingPendingReview());
    }

    @Test
    void shouldReadThePendingReviewOfThePullRequest() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());
        assertThat(service.read(project(), pullRequestNumber())).isEqualTo(new ReadPendingReview(reviewedPullRequest()));
    }

    @Test
    void shouldWriteACommentThroughThePort() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());
        assertThat(service.comment(project(), pullRequestNumber(), onDiffLine(), commentBody()))
                .isEqualTo(new WrittenReview(new ReadPendingReview(reviewedPullRequest())));
        verify(reviews).addThread(project(), pendingReviewId(), onDiffLine(), commentBody());
    }

    @Test
    void shouldWriteAReplyAnEditADeletionASummaryAndASubmitThroughThePort() {
        when(reviews.pullRequest(project(), pullRequestNumber())).thenReturn(reviewedPullRequest());
        service.reply(project(), pullRequestNumber(), threadId(), commentBody());
        service.edit(project(), pullRequestNumber(), pendingCommentId(), commentBody());
        service.delete(project(), pullRequestNumber(), pendingCommentId());
        service.summarise(project(), pullRequestNumber(), summary());
        service.submit(project(), pullRequestNumber());

        verify(reviews).reply(project(), pendingReviewId(), threadId(), commentBody());
        verify(reviews).edit(project(), pendingCommentId(), commentBody());
        verify(reviews).delete(project(), pendingCommentId());
        verify(reviews).summarise(project(), pendingReviewId(), summary());
        verify(reviews).submit(project(), pendingReviewId());
    }

    @Test
    void shouldAnchorACommentAndPlaceThePendingCommentsOnTheDiff() {
        ReadDiff diff = new ReadDiff(pullRequestDiffOf(modifiedFile()));

        assertThat(service.anchor(diff, new DiffLineIndex(2), new DiffLineIndex(2))).isEqualTo(onDiff(diffPath(), rightOn(3)));
        assertThat(service.pendingInDiff(diff, List.of(commentOn(diffPath(), rightOn(3)))).comments())
                .containsExactly(PendingReviewFixture.onDiffLine(commentOn(diffPath(), rightOn(3)), 2));
    }
}

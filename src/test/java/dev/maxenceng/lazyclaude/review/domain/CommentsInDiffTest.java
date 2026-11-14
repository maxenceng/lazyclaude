package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.commentOnDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingAtFileHead;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingOutsideDiff;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.replyToThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.onLine;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.outdatedThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.outsideDiff;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.resolvedThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.reviewThread;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CommentsInDiffTest {

    @Test
    void shouldKeepResolvedThreadsFromUnderTheirLinesUnlessResolvedThreadsAreShown() {
        CommentsInDiff comments = CommentsInDiff.builder().threads(new ThreadsInDiff(List.of(onLine(reviewThread(), 2), onLine(resolvedThread(), 4)))).pending(new PendingCommentsInDiff(List.of())).build();

        assertThat(comments.shown(false)).isEqualTo(List.of(onLine(reviewThread(), 2)));
        assertThat(comments.shown(true)).isEqualTo(List.of(onLine(reviewThread(), 2), onLine(resolvedThread(), 4)));
    }

    @Test
    void shouldListThePendingCommentsFirstThenEveryThreadInDiffOrder() {
        CommentsInDiff comments = CommentsInDiff.builder().threads(new ThreadsInDiff(List.of(onLine(resolvedThread(), 1), outsideDiff(outdatedThread())))).pending(new PendingCommentsInDiff(List.of(onDiffLine(commentOnDiffLine(), 3), pendingOutsideDiff(replyToThread())))).build();

        assertThat(comments.listed())
                .isEqualTo(List.of(
                        onDiffLine(commentOnDiffLine(), 3),
                        pendingOutsideDiff(replyToThread()),
                        onLine(resolvedThread(), 1),
                        outsideDiff(outdatedThread())));
    }

    @Test
    void shouldPickPerPlaceThePendingCommentWrittenLast() {
        CommentsInDiff comments = CommentsInDiff.builder().threads(new ThreadsInDiff(List.of())).pending(new PendingCommentsInDiff(List.of(
                        onDiffLine(commentOnDiffLine(), 1), onDiffLine(replyToThread(), 1), onDiffLine(commentOnDiffLine(), 5),
                        pendingAtFileHead(commentOnDiffLine(), 0), pendingAtFileHead(replyToThread(), 0),
                        pendingOutsideDiff(commentOnDiffLine()), pendingOutsideDiff(replyToThread())))).build();

        assertThat(comments.lastAtPlaces())
                .as("a file's header and the foot after the diff are places as a line is, so their keys reach the comment written last there")
                .isEqualTo(List.of(onDiffLine(replyToThread(), 1), onDiffLine(commentOnDiffLine(), 5), pendingAtFileHead(replyToThread(), 0), pendingOutsideDiff(replyToThread())));
    }
}

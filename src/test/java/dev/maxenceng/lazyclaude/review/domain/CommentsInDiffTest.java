package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.commentOnDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onNoDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.replyToThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.onLine;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.onNoLine;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.outdatedThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.resolvedThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.reviewThread;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CommentsInDiffTest {

    @Test
    void shouldKeepResolvedThreadsFromUnderTheirLinesUnlessResolvedThreadsAreShown() {
        CommentsInDiff comments = CommentsInDiff.builder().threads(new ThreadsInDiff(List.of(onLine(reviewThread(), 2), onLine(resolvedThread(), 4)))).pending(new PendingCommentsInDiff(List.of())).build();

        assertThat(comments.underLines(false)).isEqualTo(List.of(onLine(reviewThread(), 2)));
        assertThat(comments.underLines(true)).isEqualTo(List.of(onLine(reviewThread(), 2), onLine(resolvedThread(), 4)));
    }

    @Test
    void shouldListThePendingCommentsFirstThenEveryThreadInDiffOrder() {
        CommentsInDiff comments = CommentsInDiff.builder().threads(new ThreadsInDiff(List.of(onLine(resolvedThread(), 1), onNoLine(outdatedThread())))).pending(new PendingCommentsInDiff(List.of(onDiffLine(commentOnDiffLine(), 3), onNoDiffLine(replyToThread())))).build();

        assertThat(comments.listed())
                .isEqualTo(List.of(
                        onDiffLine(commentOnDiffLine(), 3),
                        onNoDiffLine(replyToThread()),
                        onLine(resolvedThread(), 1),
                        onNoLine(outdatedThread())));
    }

    @Test
    void shouldPickPerLineThePendingCommentWrittenLastAndNoneForACommentOnNoLine() {
        CommentsInDiff comments = CommentsInDiff.builder().threads(new ThreadsInDiff(List.of())).pending(new PendingCommentsInDiff(List.of(
                        onDiffLine(commentOnDiffLine(), 1), onDiffLine(replyToThread(), 1), onDiffLine(commentOnDiffLine(), 5), onNoDiffLine(replyToThread())))).build();

        assertThat(comments.lastOnLines()).isEqualTo(List.of(onDiffLine(replyToThread(), 1), onDiffLine(commentOnDiffLine(), 5)));
    }
}

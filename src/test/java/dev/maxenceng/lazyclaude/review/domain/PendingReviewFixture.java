package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.editedCommentBody;
import static dev.maxenceng.lazyclaude.review.published.CommitShaFixture.commitSha;
import static dev.maxenceng.lazyclaude.review.published.CommitShaFixture.movedCommitSha;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.pendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.secondPendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.SummaryFixture.summary;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PendingReview.PendingReviewBuilder;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.NoDiffLine;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

public final class PendingReviewFixture {

    private PendingReviewFixture() {}

    public static PendingReviewId pendingReviewId() {
        return new PendingReviewId("PRR_1");
    }

    public static PullRequestNodeId pullRequestNodeId() {
        return new PullRequestNodeId("PR_1");
    }

    public static PendingOnDiff onDiff(DiffPath path, PlacedThread position) {
        return PendingOnDiff.builder().path(path).position(position).build();
    }

    public static PendingOnDiff onDiffLine() {
        return onDiff(diffPath(), rightOn(3));
    }

    public static PendingComment commentOnDiffLine() {
        return PendingComment.builder().id(pendingCommentId()).body(commentBody()).anchor(onDiffLine()).build();
    }

    public static PendingComment commentOn(DiffPath path, PlacedThread position) {
        return PendingComment.builder()
                .id(pendingCommentId())
                .body(commentBody())
                .anchor(onDiff(path, position))
                .build();
    }

    public static PendingComment replyToThread() {
        return PendingComment.builder()
                .id(secondPendingCommentId())
                .body(editedCommentBody())
                .anchor(replyOn(diffPath(), rightOn(3)))
                .build();
    }

    public static PendingReply replyOn(DiffPath path, ThreadPosition position) {
        return PendingReply.builder().thread(threadId()).path(path).position(position).build();
    }

    public static PendingComment replyCommentOn(DiffPath path, ThreadPosition position) {
        return PendingComment.builder()
                .id(secondPendingCommentId())
                .body(editedCommentBody())
                .anchor(replyOn(path, position))
                .build();
    }

    public static PendingInDiff onDiffLine(PendingComment comment, int index) {
        return PendingInDiff.builder().comment(comment).line(new OnDiffLine(new DiffLineIndex(index))).build();
    }

    public static PendingInDiff onNoDiffLine(PendingComment comment) {
        return PendingInDiff.builder().comment(comment).line(new NoDiffLine()).build();
    }

    public static PendingReviewBuilder pendingReviewBuilder() {
        return PendingReview.builder()
                .id(pendingReviewId())
                .writtenOn(commitSha())
                .summary(summary())
                .comments(List.of(commentOnDiffLine(), replyToThread()));
    }

    public static PendingReview pendingReview() {
        return pendingReviewBuilder().build();
    }

    public static ReviewedPullRequest reviewedPullRequestWith(ReviewInProgress review) {
        return ReviewedPullRequest.builder().id(pullRequestNodeId()).head(commitSha()).review(review).build();
    }

    public static ReviewedPullRequest reviewedPullRequest() {
        return reviewedPullRequestWith(pendingReview());
    }

    public static ReviewedPullRequest movedReviewedPullRequest() {
        return ReviewedPullRequest.builder().id(pullRequestNodeId()).head(movedCommitSha()).review(pendingReview()).build();
    }

    public static ReviewedPullRequest unreviewedPullRequest() {
        return reviewedPullRequestWith(new NoPendingReview());
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.editedCommentBody;
import static dev.maxenceng.lazyclaude.review.published.CommitShaFixture.commitSha;
import static dev.maxenceng.lazyclaude.review.published.CommitShaFixture.movedCommitSha;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.pendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.secondPendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.review.published.SummaryFixture.summary;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;

import java.util.List;

import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.HeadMove;
import dev.maxenceng.lazyclaude.review.published.HeldHead;
import dev.maxenceng.lazyclaude.review.published.MovedHead;
import dev.maxenceng.lazyclaude.review.published.NoDiffLine;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

public final class PendingReviewViewFixture {

    private PendingReviewViewFixture() {}

    public static LoadingPendingReviewView loadingPendingView() {
        return new LoadingPendingReviewView();
    }

    public static NoPendingReviewView noPendingReviewView() {
        return new NoPendingReviewView();
    }

    public static UnavailablePendingReviewView unavailablePendingView() {
        return new UnavailablePendingReviewView(reason());
    }

    public static OnDiffAnchorView onDiffAnchorView(DiffPath path, PlacedThread position) {
        return OnDiffAnchorView.builder().path(path).position(position).build();
    }

    public static ReplyAnchorView replyAnchorView(DiffPath path, ThreadPosition position) {
        return ReplyAnchorView.builder().thread(threadId()).path(path).position(position).build();
    }

    public static PendingCommentView pendingCommentView(PendingCommentId id, CommentBody body, PendingAnchorView anchor) {
        return PendingCommentView.builder().id(id).body(body).anchor(anchor).build();
    }

    public static PendingCommentView commentOnDiffLineView() {
        return pendingCommentView(pendingCommentId(), commentBody(), onDiffAnchorView(diffPath(), rightOn(3)));
    }

    public static PendingCommentView commentOnView(DiffPath path, PlacedThread position) {
        return pendingCommentView(pendingCommentId(), commentBody(), onDiffAnchorView(path, position));
    }

    public static PendingCommentView replyView() {
        return replyViewOn(diffPath(), rightOn(3));
    }

    public static PendingCommentView replyViewOn(DiffPath path, ThreadPosition position) {
        return pendingCommentView(secondPendingCommentId(), editedCommentBody(), replyAnchorView(path, position));
    }

    public static OpenPendingReviewView openPendingReviewView(HeadMove headMove, Summary summary, PendingCommentView... comments) {
        return OpenPendingReviewView.builder().summary(summary).comments(List.of(comments)).headMove(headMove).build();
    }

    public static OpenPendingReviewView openPendingView() {
        return openPendingReviewView(new HeldHead(), summary(), commentOnDiffLineView(), replyView());
    }

    public static OpenPendingReviewView openPendingViewOf(PendingCommentView... comments) {
        return openPendingReviewView(new HeldHead(), summary(), comments);
    }

    public static OpenPendingReviewView movedOpenPendingView() {
        return openPendingReviewView(
                MovedHead.builder()
                        .writtenOn(commitSha())
                        .head(movedCommitSha())
                        .build(),
                summary(),
                commentOnDiffLineView(),
                replyView());
    }

    public static WrittenReviewView writtenView(PendingReviewView pending) {
        return new WrittenReviewView(pending);
    }

    public static RefusedWriteView refusedWriteView() {
        return new RefusedWriteView(reason());
    }

    public static PendingInDiffView pendingOnLineView(PendingCommentView comment, int index) {
        return PendingInDiffView.builder().comment(comment).line(new OnDiffLine(new DiffLineIndex(index))).build();
    }

    public static PendingInDiffView pendingOnNoLineView(PendingCommentView comment) {
        return PendingInDiffView.builder().comment(comment).line(new NoDiffLine()).build();
    }

    public static PendingCommentPayload pendingCommentPayload() {
        return new PendingCommentPayload(project().root(), pullRequestNumber(), diffPath(), rightOn(3), commentBody());
    }

    public static PendingReplyPayload pendingReplyPayload() {
        return new PendingReplyPayload(project().root(), pullRequestNumber(), threadId(), commentBody());
    }

    public static PendingEditPayload pendingEditPayload() {
        return new PendingEditPayload(project().root(), pullRequestNumber(), pendingCommentId(), editedCommentBody());
    }

    public static PendingDeletePayload pendingDeletePayload() {
        return new PendingDeletePayload(project().root(), pullRequestNumber(), pendingCommentId());
    }

    public static PendingSubmitPayload pendingSubmitPayload() {
        return new PendingSubmitPayload(project().root(), pullRequestNumber(), summary());
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.application.PendingReviewApplicationService;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * What the shell asks of {@code review} to write your pending review of a pull request, which GitHub keeps until it is submitted
 * (ADR 0009): a comment, a reply, an edit, a deletion, the summary, and the submit. Each waits for {@code gh}, so the caller is off the
 * render thread, and answers what GitHub took or why it did not; it never fails.
 */
public final class PendingReviewCommand {

    private final PendingReviewApplicationService pendingReview;

    public PendingReviewCommand(PendingReviewApplicationService pendingReview) {
        this.pendingReview = pendingReview;
    }

    public PendingReviewWriteView comment(PendingCommentPayload payload) {
        return PendingReviewWriteView.from(
                pendingReview.comment(new Project(payload.root()), payload.number(), payload.where(), payload.body()));
    }

    public PendingReviewWriteView reply(PendingReplyPayload payload) {
        return PendingReviewWriteView.from(
                pendingReview.reply(new Project(payload.root()), payload.number(), payload.thread(), payload.body()));
    }

    /** Saving a comment with nothing in it deletes it. */
    public PendingReviewWriteView edit(PendingEditPayload payload) {
        return PendingReviewWriteView.from(
                pendingReview.edit(new Project(payload.root()), payload.number(), payload.comment(), payload.body()));
    }

    public PendingReviewWriteView delete(PendingDeletePayload payload) {
        return PendingReviewWriteView.from(pendingReview.delete(new Project(payload.root()), payload.number(), payload.comment()));
    }

    public PendingReviewWriteView summarise(PendingSummaryPayload payload) {
        return PendingReviewWriteView.from(pendingReview.summarise(new Project(payload.root()), payload.number(), payload.summary()));
    }

    /** Sends the pending review as a Comment review. GitHub is the one to refuse it. */
    public PendingReviewWriteView submit(PullRequestPayload payload) {
        return PendingReviewWriteView.from(pendingReview.submit(payload.project(), payload.number()));
    }
}

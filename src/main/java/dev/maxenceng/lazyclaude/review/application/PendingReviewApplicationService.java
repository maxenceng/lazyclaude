package dev.maxenceng.lazyclaude.review.application;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.DiffReading;
import dev.maxenceng.lazyclaude.review.domain.PendingComment;
import dev.maxenceng.lazyclaude.review.domain.PendingCommentsInDiff;
import dev.maxenceng.lazyclaude.review.domain.PendingOnDiff;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewManager;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewPort;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewReading;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewWrite;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads and writes your pending review of a pull request, each when asked. */
public final class PendingReviewApplicationService {

    private final PendingReviewManager manager;

    public PendingReviewApplicationService(PendingReviewPort reviews) {
        this.manager = new PendingReviewManager(reviews);
    }

    public PendingReviewReading loading() {
        return manager.loading();
    }

    public PendingReviewReading read(Project project, PullRequestNumber number) {
        return manager.read(project, number);
    }

    public PendingCommentsInDiff pendingInDiff(DiffReading diff, List<PendingComment> comments) {
        return manager.pendingInDiff(diff, comments);
    }

    public PendingOnDiff anchor(DiffReading diff, DiffLineIndex first, DiffLineIndex last) {
        return manager.anchor(diff, first, last);
    }

    public PendingReviewWrite comment(Project project, PullRequestNumber number, PendingOnDiff where, CommentBody body) {
        return manager.comment(project, number, where, body);
    }

    public PendingReviewWrite reply(Project project, PullRequestNumber number, ThreadId thread, CommentBody body) {
        return manager.reply(project, number, thread, body);
    }

    public PendingReviewWrite edit(Project project, PullRequestNumber number, PendingCommentId comment, CommentBody body) {
        return manager.edit(project, number, comment, body);
    }

    public PendingReviewWrite delete(Project project, PullRequestNumber number, PendingCommentId comment) {
        return manager.delete(project, number, comment);
    }

    public PendingReviewWrite submit(Project project, PullRequestNumber number, Summary summary) {
        return manager.submit(project, number, summary);
    }
}

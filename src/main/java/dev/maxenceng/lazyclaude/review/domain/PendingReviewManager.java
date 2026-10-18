package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.NoDiffLine;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Reason;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads and writes your pending review of a pull request when asked. It owns the rules that a read or a write which fails is answered
 * with its reason, never swallowed, that a write which GitHub took is answered with the pending review read again after it, and that
 * the first thing written to a pull request starts its pending review.
 */
public record PendingReviewManager(PendingReviewPort reviews) {

    private static final String NOTHING_TO_SUBMIT = "the pull request has no pending review to submit";

    /** What stands until the pending review's read answers. */
    public PendingReviewReading loading() {
        return new LoadingPendingReview();
    }

    public PendingReviewReading read(Project project, PullRequestNumber number) {
        try {
            return new ReadPendingReview(reviews.pullRequest(project, number));
        } catch (PullRequestsUnavailableException noAnswer) {
            return new UnavailablePendingReview(new Reason(noAnswer.getMessage()));
        }
    }

    /** Adds a comment on the diff to the pending review; an empty one writes nothing. */
    public PendingReviewWrite comment(Project project, PullRequestNumber number, PendingOnDiff where, CommentBody body) {
        return written(project, number, () -> {
            if (!body.isBlank()) {
                reviews.addThread(project, reviewToWriteTo(project, number), where, body);
            }
        });
    }

    /** Adds a reply to {@code thread} to the pending review; an empty one writes nothing. */
    public PendingReviewWrite reply(Project project, PullRequestNumber number, ThreadId thread, CommentBody body) {
        return written(project, number, () -> {
            if (!body.isBlank()) {
                reviews.reply(project, reviewToWriteTo(project, number), thread, body);
            }
        });
    }

    /** Replaces what a pending comment says; one left empty is deleted, since an empty comment is not worth sending. */
    public PendingReviewWrite edit(Project project, PullRequestNumber number, PendingCommentId comment, CommentBody body) {
        return written(project, number, () -> {
            if (body.isBlank()) {
                reviews.delete(project, comment);
            } else {
                reviews.edit(project, comment, body);
            }
        });
    }

    public PendingReviewWrite delete(Project project, PullRequestNumber number, PendingCommentId comment) {
        return written(project, number, () -> reviews.delete(project, comment));
    }

    /** Sets the pending review's summary, starting the review if it has none yet. */
    public PendingReviewWrite summarise(Project project, PullRequestNumber number, Summary summary) {
        return written(project, number, () -> reviews.summarise(project, reviewToWriteTo(project, number), summary));
    }

    /** Sends the pending review as a Comment review. GitHub is the one to refuse it: nothing is checked here but that there is one. */
    public PendingReviewWrite submit(Project project, PullRequestNumber number) {
        try {
            return switch (reviews.pullRequest(project, number).review()) {
                case PendingReview pending -> {
                    reviews.submit(project, pending.id());
                    yield new WrittenReview(read(project, number));
                }
                case NoPendingReview _ -> new RefusedWrite(new Reason(NOTHING_TO_SUBMIT));
            };
        } catch (PullRequestsUnavailableException refused) {
            return new RefusedWrite(new Reason(refused.getMessage()));
        }
    }

    /**
     * The pending comments with the diff lines they show under, from whatever each read last answered: none until the pending review is
     * read, and every one on no line until the diff is.
     */
    public PendingCommentsInDiff pendingInDiff(DiffReading diff, PendingReviewReading pending) {
        if (!(pending instanceof ReadPendingReview read) || !(read.pullRequest().review() instanceof PendingReview review)) {
            return new PendingCommentsInDiff(List.of());
        }
        if (diff instanceof ReadDiff readDiff) {
            return readDiff.diff().place(review);
        }
        return new PendingCommentsInDiff(review.comments().stream()
                .map(comment -> PendingInDiff.builder().comment(comment).line(new NoDiffLine()).build())
                .toList());
    }

    /** The comment on the diff line at {@code first}, or on the range from it to {@code last}; there is no line to comment on until the diff is read. */
    public PendingOnDiff anchor(DiffReading diff, DiffLineIndex first, DiffLineIndex last) {
        if (diff instanceof ReadDiff read) {
            return read.diff().commentOn(first, last);
        }
        throw new DiffLineMissingException();
    }

    private PendingReviewWrite written(Project project, PullRequestNumber number, Runnable write) {
        try {
            write.run();
            return new WrittenReview(read(project, number));
        } catch (PullRequestsUnavailableException refused) {
            return new RefusedWrite(new Reason(refused.getMessage()));
        }
    }

    /** The pending review as GitHub has it now, started if the pull request has none yet. */
    private PendingReviewId reviewToWriteTo(Project project, PullRequestNumber number) {
        ReviewedPullRequest pullRequest = reviews.pullRequest(project, number);
        return switch (pullRequest.review()) {
            case PendingReview pending -> pending.id();
            case NoPendingReview _ -> reviews.create(project, pullRequest.id());
        };
    }
}

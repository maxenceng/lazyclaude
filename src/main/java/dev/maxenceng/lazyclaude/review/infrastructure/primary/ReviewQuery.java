package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.review.application.PendingReviewApplicationService;
import dev.maxenceng.lazyclaude.review.application.PullRequestDiffApplicationService;
import dev.maxenceng.lazyclaude.review.application.ReviewApplicationService;
import dev.maxenceng.lazyclaude.review.published.Reason;

/**
 * What the shell asks of {@code review}: the open and the merged pull requests of a project as views, and a view again after each fetch,
 * and one pull request's diff and threads when it asks for them. It only reads.
 */
public final class ReviewQuery {

    private final ReviewApplicationService review;
    private final PullRequestDiffApplicationService pullRequestDiff;
    private final PendingReviewApplicationService pendingReview;

    public ReviewQuery(
            ReviewApplicationService review,
            PullRequestDiffApplicationService pullRequestDiff,
            PendingReviewApplicationService pendingReview) {
        this.review = review;
        this.pullRequestDiff = pullRequestDiff;
        this.pendingReview = pendingReview;
    }

    /** What stands until the diff's read answers. */
    public PullRequestDiffView loadingDiff() {
        return PullRequestDiffView.from(pullRequestDiff.loadingDiff());
    }

    /** What stands until the threads' read answers. */
    public ThreadsView loadingThreads() {
        return ThreadsView.from(pullRequestDiff.loadingThreads());
    }

    /** Reads the pull request's diff now, on the caller's thread, which waits for {@code gh}; never a failure, the reason is the view. */
    public PullRequestDiffView diffOf(PullRequestPayload payload) {
        return PullRequestDiffView.from(pullRequestDiff.diff(payload.project(), payload.number()));
    }

    /** Reads the pull request's threads now, on the caller's thread, which waits for {@code gh}; never a failure, the reason is the view. */
    public ThreadsView threadsOf(PullRequestPayload payload) {
        return ThreadsView.from(pullRequestDiff.threads(payload.project(), payload.number()));
    }

    /**
     * The threads with the diff lines they show under, in diff order, from whatever each read last answered: none until the threads are
     * read, and every one on no line until the diff is. It reads nothing.
     */
    public ThreadsInDiffView threadsInDiff(ThreadsInDiffPayload payload) {
        return ThreadsInDiffView.from(pullRequestDiff.threadsInDiff(
                ReviewReadings.diffOf(payload.diff()), ReviewReadings.threadsOf(payload.threads())));
    }

    /** What stands until the pending review's read answers. */
    public PendingReviewView loadingPending() {
        return PendingReviewView.from(pendingReview.loading());
    }

    /** Reads your pending review of the pull request now, on the caller's thread, which waits for {@code gh}; never a failure, the reason is the view. */
    public PendingReviewView pendingOf(PullRequestPayload payload) {
        return PendingReviewView.from(pendingReview.read(payload.project(), payload.number()));
    }

    /**
     * Where a comment on the diff lines from {@code first} to {@code last} would hang, or why it cannot: the range runs out of its hunk,
     * or a line is not in the diff. It reads nothing.
     */
    public CommentAnchorView anchorOf(CommentAnchorPayload payload) {
        try {
            return OnDiffAnchorView.from(pendingReview.anchor(ReviewReadings.diffOf(payload.diff()), payload.first(), payload.last()));
        } catch (DomainException refused) {
            return new RefusedAnchorView(new Reason(refused.getMessage()));
        }
    }

    /**
     * The pending comments with the diff lines they show under, in diff order, from whatever each read last answered: none until the
     * pending review is read, and every one on no line until the diff is. It reads nothing.
     */
    public PendingCommentsInDiffView pendingInDiff(PendingInDiffPayload payload) {
        return PendingCommentsInDiffView.from(pendingReview.pendingInDiff(
                ReviewReadings.diffOf(payload.diff()), ReviewReadings.commentsOf(payload.pending())));
    }

    /** What stands until the first fetch answers. */
    public PullRequestsView checking() {
        return PullRequestsView.from(review.checking());
    }

    /** Fetches again now rather than at the next interval; a fetch already under way answers it. */
    public void refresh() {
        review.runNow();
    }

    /**
     * Hands {@code listener} the open pull requests and {@code mergedListener} the merged ones after each fetch, the first at once,
     * on a thread of the watcher's own, never a failure.
     */
    public void subscribe(
            ReviewPayload payload, Consumer<PullRequestsView> listener, Consumer<MergedPullRequestsView> mergedListener) {
        review.subscribe(
                payload.toDomain(),
                read -> listener.accept(PullRequestsView.from(read)),
                merged -> mergedListener.accept(MergedPullRequestsView.from(merged)));
    }
}

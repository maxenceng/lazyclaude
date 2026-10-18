package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.review.application.PendingReviewApplicationService;
import dev.maxenceng.lazyclaude.review.application.PullRequestDiffApplicationService;
import dev.maxenceng.lazyclaude.review.application.ReviewApplicationService;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;
import dev.maxenceng.lazyclaude.review.domain.ThreadPort;

public final class ReviewQueryFixture {

    private ReviewQueryFixture() {}

    public static ReviewQuery reviewQuery(
            OpenPullRequestPort pullRequests, MergedPullRequestPort mergedPullRequests, PullRequestWatchPort watch) {
        return reviewQuery(pullRequests, mergedPullRequests, watch, mock(PullRequestDiffPort.class), mock(ThreadPort.class));
    }

    public static ReviewQuery reviewQuery(PullRequestDiffPort diffs, ThreadPort threads) {
        return reviewQuery(
                mock(OpenPullRequestPort.class), mock(MergedPullRequestPort.class), mock(PullRequestWatchPort.class), diffs, threads);
    }

    public static ReviewQuery reviewQuery(PendingReviewPort pending) {
        return new ReviewQuery(
                new ReviewApplicationService(
                        mock(OpenPullRequestPort.class), mock(MergedPullRequestPort.class), mock(PullRequestWatchPort.class)),
                new PullRequestDiffApplicationService(mock(PullRequestDiffPort.class), mock(ThreadPort.class)),
                new PendingReviewApplicationService(pending));
    }

    public static PendingReviewCommand pendingReviewCommand(PendingReviewPort pending) {
        return new PendingReviewCommand(new PendingReviewApplicationService(pending));
    }

    private static ReviewQuery reviewQuery(
            OpenPullRequestPort pullRequests,
            MergedPullRequestPort mergedPullRequests,
            PullRequestWatchPort watch,
            PullRequestDiffPort diffs,
            ThreadPort threads) {
        return new ReviewQuery(
                new ReviewApplicationService(pullRequests, mergedPullRequests, watch),
                new PullRequestDiffApplicationService(diffs, threads),
                new PendingReviewApplicationService(mock(PendingReviewPort.class)));
    }
}

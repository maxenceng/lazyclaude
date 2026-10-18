package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.review.application.PullRequestReviewApplicationService;
import dev.maxenceng.lazyclaude.review.application.ReviewApplicationService;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
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

    private static ReviewQuery reviewQuery(
            OpenPullRequestPort pullRequests,
            MergedPullRequestPort mergedPullRequests,
            PullRequestWatchPort watch,
            PullRequestDiffPort diffs,
            ThreadPort threads) {
        return new ReviewQuery(
                new ReviewApplicationService(pullRequests, mergedPullRequests, watch),
                new PullRequestReviewApplicationService(diffs, threads));
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.application.ReviewApplicationService;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;

public final class ReviewQueryFixture {

    private ReviewQueryFixture() {}

    public static ReviewQuery reviewQuery(OpenPullRequestPort pullRequests, PullRequestWatchPort watch) {
        return new ReviewQuery(new ReviewApplicationService(pullRequests, watch));
    }
}

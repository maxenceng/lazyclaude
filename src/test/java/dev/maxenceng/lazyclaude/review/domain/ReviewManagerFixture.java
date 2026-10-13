package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.mockito.ArgumentCaptor;

public final class ReviewManagerFixture {

    private ReviewManagerFixture() {}

    public static PullRequestsUnavailableException pullRequestsUnavailable() {
        return new PullRequestsUnavailableException(reason().value());
    }

    public static OpenPullRequests openPullRequests() {
        return new OpenPullRequests(List.of(PullRequestFixture.pullRequest()));
    }

    public static UnavailablePullRequests unavailablePullRequests() {
        return new UnavailablePullRequests(reason());
    }

    public static Runnable watchedRefresh(PullRequestWatchPort watch) {
        ArgumentCaptor<Runnable> refresh = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(refresh.capture());
        return refresh.getValue();
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.CheckingPullRequests;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequests;
import dev.maxenceng.lazyclaude.review.domain.PullRequests;
import dev.maxenceng.lazyclaude.review.domain.UnavailablePullRequests;

/** The open pull requests as the shell reads them: not fetched yet, the open ones, or why GitHub could not be read. */
public sealed interface PullRequestsView permits CheckingPullRequestsView, OpenPullRequestsView, UnavailablePullRequestsView {

    static PullRequestsView from(PullRequests pullRequests) {
        return switch (pullRequests) {
            case CheckingPullRequests _ -> new CheckingPullRequestsView();
            case OpenPullRequests open -> OpenPullRequestsView.from(open);
            case UnavailablePullRequests unavailable -> new UnavailablePullRequestsView(unavailable.reason());
        };
    }
}

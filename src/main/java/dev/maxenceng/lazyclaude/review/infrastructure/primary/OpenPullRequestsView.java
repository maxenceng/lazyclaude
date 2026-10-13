package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.review.domain.OpenPullRequests;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;

/** The project's open pull requests, draft ones included. */
public record OpenPullRequestsView(List<PullRequestView> pullRequests) implements PullRequestsView {

    static OpenPullRequestsView from(OpenPullRequests open) {
        return new OpenPullRequestsView(
                open.pullRequests().stream().map(PullRequestView::from).toList());
    }

    /** The pull request whose head is exactly {@code branch}. */
    public Optional<PullRequestView> on(HeadBranch branch) {
        return pullRequests.stream().filter(pullRequest -> pullRequest.head().equals(branch)).findFirst();
    }
}

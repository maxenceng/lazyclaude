package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where the project's open pull requests are read from. */
public interface OpenPullRequestPort {

    /**
     * Every open pull request of {@code project}'s repository, draft ones included, with its checks.
     *
     * @throws PullRequestsUnavailableException when GitHub cannot be read, with the reason
     */
    List<PullRequest> openPullRequests(Project project);
}

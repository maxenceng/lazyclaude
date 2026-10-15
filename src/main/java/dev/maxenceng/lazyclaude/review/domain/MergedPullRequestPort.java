package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where the project's merged pull requests are read from. */
public interface MergedPullRequestPort {

    /**
     * The most recent merged pull requests of {@code project}'s repository, none of them from a fork. Older ones are not answered, which
     * is a limit of the fetch, not a fact about the repository.
     *
     * @throws PullRequestsUnavailableException when GitHub cannot be read, with the reason
     */
    List<MergedPullRequest> mergedPullRequests(Project project);
}

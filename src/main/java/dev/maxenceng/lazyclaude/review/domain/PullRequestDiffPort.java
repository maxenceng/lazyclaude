package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a pull request's diff is read from. */
public interface PullRequestDiffPort {

    /**
     * The unified diff of pull request {@code number} of {@code project}'s repository, every file of it.
     *
     * @throws PullRequestDiffTooLargeException when GitHub refuses to produce a diff so large
     * @throws PullRequestsUnavailableException when GitHub cannot be read, with the reason
     */
    PullRequestDiff diff(Project project, PullRequestNumber number);
}

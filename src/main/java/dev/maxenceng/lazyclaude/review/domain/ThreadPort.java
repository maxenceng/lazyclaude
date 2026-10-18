package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a pull request's review threads are read from. */
public interface ThreadPort {

    /**
     * Every review thread of pull request {@code number} of {@code project}'s repository, resolved and outdated ones included.
     *
     * @throws PullRequestsUnavailableException when GitHub cannot be read, with the reason
     */
    List<ReviewThread> threads(Project project, PullRequestNumber number);
}

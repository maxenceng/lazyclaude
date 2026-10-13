package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.Reason;

/** GitHub could not be read, and why. */
public record UnavailablePullRequestsView(Reason reason) implements PullRequestsView {}

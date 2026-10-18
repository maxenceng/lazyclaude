package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.Reason;

/** The threads could not be read, and why. */
public record UnavailableThreadsView(Reason reason) implements ThreadsView {}

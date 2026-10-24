package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.Reason;

/** The pending review could not be read, and why. */
public record UnavailablePendingReviewView(Reason reason) implements PendingReviewView {}

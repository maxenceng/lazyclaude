package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.Reason;

/** GitHub did not take the write, or did not answer, and why; nothing saved before was lost. */
public record RefusedWriteView(Reason reason) implements PendingReviewWriteView {}

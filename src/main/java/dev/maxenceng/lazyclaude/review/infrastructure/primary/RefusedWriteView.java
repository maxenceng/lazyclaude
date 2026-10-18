package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.Reason;

/** GitHub did not take the write, and why; nothing was lost, and writing again is safe. */
public record RefusedWriteView(Reason reason) implements PendingReviewWriteView {}

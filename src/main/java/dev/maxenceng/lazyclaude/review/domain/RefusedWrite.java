package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** GitHub did not take the write, offline, unauthenticated or by a refusal of its own; nothing was lost, and the reason says why. */
public record RefusedWrite(Reason reason) implements PendingReviewWrite {

    public RefusedWrite {
        Assert.notNull("reason", reason);
    }
}

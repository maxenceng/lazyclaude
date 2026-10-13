package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** GitHub could not be read; lazyclaude starts and runs anyway, and says why. */
public record UnavailablePullRequests(Reason reason) implements PullRequests {

    public UnavailablePullRequests {
        Assert.notNull("reason", reason);
    }
}

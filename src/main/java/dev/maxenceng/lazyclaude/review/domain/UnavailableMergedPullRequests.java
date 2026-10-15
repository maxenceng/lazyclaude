package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** GitHub could not be read for the merged pull requests; lazyclaude runs anyway, and says why. */
public record UnavailableMergedPullRequests(Reason reason) implements MergedPullRequests {

    public UnavailableMergedPullRequests {
        Assert.notNull("reason", reason);
    }
}

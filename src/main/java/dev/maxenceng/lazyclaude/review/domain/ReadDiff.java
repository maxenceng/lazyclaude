package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The diff GitHub answered with. */
public record ReadDiff(PullRequestDiff diff) implements DiffReading {

    public ReadDiff {
        Assert.notNull("diff", diff);
    }
}

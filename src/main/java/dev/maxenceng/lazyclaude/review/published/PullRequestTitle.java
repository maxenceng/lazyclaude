package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A pull request's title, as GitHub shows it. */
public record PullRequestTitle(String value) {

    public PullRequestTitle {
        Assert.notBlank("pull request title", value);
    }
}

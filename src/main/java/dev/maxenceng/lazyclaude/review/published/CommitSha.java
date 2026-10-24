package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A commit of a pull request: the one its head is at, or the one the pending review was written on. */
public record CommitSha(String value) {

    private static final int ABBREVIATED_LENGTH = 7;

    public CommitSha {
        Assert.notBlank("commit", value);
    }

    /** The first seven characters, as GitHub shows a commit. */
    public String abbreviated() {
        return value.substring(0, Math.min(ABBREVIATED_LENGTH, value.length()));
    }
}

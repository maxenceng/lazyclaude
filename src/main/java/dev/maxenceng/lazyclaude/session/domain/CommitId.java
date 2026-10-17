package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A commit, by the hash git names it with. */
public record CommitId(String value) {

    public CommitId {
        Assert.notBlank("commit", value);
    }
}

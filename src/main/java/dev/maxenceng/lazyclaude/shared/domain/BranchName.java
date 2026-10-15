package dev.maxenceng.lazyclaude.shared.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The name of a git branch, as the contexts that share a repository spell it. */
public record BranchName(String value) {

    public BranchName {
        Assert.notBlank("branch name", value);
    }
}

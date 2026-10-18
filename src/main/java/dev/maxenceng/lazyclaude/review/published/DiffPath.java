package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A file's path in a pull request's diff, as it stands on the pull request's head, or as it stood on its base for a deleted file. */
public record DiffPath(String value) {

    public DiffPath {
        Assert.notBlank("value", value);
    }
}

package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The pull request moves the file from the path it had. */
public record RenamedFile(DiffPath from) implements FileStatus {

    public RenamedFile {
        Assert.notNull("from", from);
    }
}

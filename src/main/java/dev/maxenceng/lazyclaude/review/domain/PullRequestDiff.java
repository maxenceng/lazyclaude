package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The unified diff of a pull request, every file in one scroll. */
public record PullRequestDiff(List<DiffFile> files) {

    public PullRequestDiff {
        Assert.notNull("files", files);
    }
}

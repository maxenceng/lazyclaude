package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The pending comments in diff order, each with its line: by the line they show under, the ones on no line last, in the order GitHub lists them. */
public record PendingCommentsInDiff(List<PendingInDiff> comments) {

    public PendingCommentsInDiff {
        Assert.notNull("comments", comments);
    }
}

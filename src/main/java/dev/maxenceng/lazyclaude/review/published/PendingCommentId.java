package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** GitHub's id of a comment of the pending review, which an edit or a deletion is addressed to. */
public record PendingCommentId(String value) {

    public PendingCommentId {
        Assert.notBlank("pending comment id", value);
    }
}

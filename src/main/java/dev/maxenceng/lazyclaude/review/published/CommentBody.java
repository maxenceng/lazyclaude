package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a comment says, as written. */
public record CommentBody(String value) {

    public CommentBody {
        Assert.notNull("value", value);
    }
}

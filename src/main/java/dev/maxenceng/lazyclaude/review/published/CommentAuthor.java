package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Who wrote a comment, by their GitHub login. */
public record CommentAuthor(String value) {

    public CommentAuthor {
        Assert.notBlank("value", value);
    }
}

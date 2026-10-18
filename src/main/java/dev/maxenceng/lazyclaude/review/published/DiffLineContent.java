package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a diff line holds after its marker; it can be empty. */
public record DiffLineContent(String value) {

    public DiffLineContent {
        Assert.notNull("value", value);
    }
}

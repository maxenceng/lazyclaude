package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The {@code @@} line that opens a hunk, as the diff writes it. */
public record HunkHeader(String value) {

    public HunkHeader {
        Assert.notBlank("value", value);
    }
}

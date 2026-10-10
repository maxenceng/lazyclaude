package dev.maxenceng.lazyclaude.docs.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A document's markdown, as written. */
public record Content(String value) implements DocumentText {

    public Content {
        Assert.notBlank("content", value);
    }
}

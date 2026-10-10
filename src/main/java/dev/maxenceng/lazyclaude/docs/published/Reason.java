package dev.maxenceng.lazyclaude.docs.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why a document could not be read, written for the person reading the main view. */
public record Reason(String value) {

    public Reason {
        Assert.notBlank("reason", value);
    }
}

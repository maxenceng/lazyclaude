package dev.maxenceng.lazyclaude.docs.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why a document or the docs tree could not be read, or why the docs could not be watched, written for the person reading the screen. */
public record Reason(String value) {

    public Reason {
        Assert.notBlank("reason", value);
    }
}

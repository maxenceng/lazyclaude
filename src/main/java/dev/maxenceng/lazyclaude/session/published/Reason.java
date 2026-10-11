package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why sessions could not be read, or could not be read the usual way, written for the person reading the screen. */
public record Reason(String value) {

    public Reason {
        Assert.notBlank("reason", value);
    }
}

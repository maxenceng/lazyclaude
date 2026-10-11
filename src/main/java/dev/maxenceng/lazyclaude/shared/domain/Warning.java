package dev.maxenceng.lazyclaude.shared.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A degraded mode lazyclaude carries on in, named once in the words a person reads. */
public record Warning(String text) {

    public Warning {
        Assert.notBlank("warning", text);
    }
}

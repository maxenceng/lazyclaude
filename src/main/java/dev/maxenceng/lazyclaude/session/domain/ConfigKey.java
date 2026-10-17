package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The name of an option in {@code kitty.conf}. */
public record ConfigKey(String value) {

    public ConfigKey {
        Assert.notBlank("config key", value);
    }
}

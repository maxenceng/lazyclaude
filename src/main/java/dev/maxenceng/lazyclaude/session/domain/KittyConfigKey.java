package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The name of an option in {@code kitty.conf}. */
public record KittyConfigKey(String value) {

    public KittyConfigKey {
        Assert.notBlank("kitty config key", value);
    }
}

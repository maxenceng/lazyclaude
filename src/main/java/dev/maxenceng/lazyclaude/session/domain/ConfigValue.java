package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What an option in {@code kitty.conf} is set to, as the file writes it. */
public record ConfigValue(String value) {

    public ConfigValue {
        Assert.notBlank("config value", value);
    }
}

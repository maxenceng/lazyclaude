package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What an option in {@code kitty.conf} is set to, as the file writes it. */
public record KittyConfigValue(String value) {

    public KittyConfigValue {
        Assert.notBlank("kitty config value", value);
    }
}

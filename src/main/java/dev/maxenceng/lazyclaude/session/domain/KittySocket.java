package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Where a kitty listens for remote control, as {@code kitten @ --to} is given it. */
public record KittySocket(String value) {

    public KittySocket {
        Assert.notBlank("kitty socket", value);
    }
}

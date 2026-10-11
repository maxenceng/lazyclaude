package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A key that types no character: an arrow, Enter, a function key. */
public record PressedKey(NamedKey key) implements KeyInput {

    public PressedKey {
        Assert.notNull("key", key);
    }
}

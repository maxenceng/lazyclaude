package dev.maxenceng.lazyclaude.session.published;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The modifier keys held with a key or a mouse button. */
public record Modifiers(List<Modifier> held) {

    public Modifiers {
        Assert.notNull("held modifiers", held);
    }

    public static Modifiers none() {
        return new Modifiers(List.of());
    }

    public static Modifiers of(Modifier... held) {
        return new Modifiers(List.of(held));
    }

    public boolean has(Modifier modifier) {
        return held.contains(modifier);
    }

    public boolean isEmpty() {
        return held.isEmpty();
    }
}

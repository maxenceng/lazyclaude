package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One key press with the modifiers held. */
public record TerminalKey(KeyInput input, Modifiers modifiers) implements TerminalInput {

    public TerminalKey {
        Assert.notNull("key input", input);
        Assert.notNull("modifiers", modifiers);
    }

    public static TerminalKeyBuilder builder() {
        return new TerminalKeyBuilder();
    }

    public static final class TerminalKeyBuilder {
        private KeyInput input;
        private Modifiers modifiers = Modifiers.none();

        private TerminalKeyBuilder() {}

        public TerminalKeyBuilder input(KeyInput input) {
            this.input = input;
            return this;
        }

        public TerminalKeyBuilder modifiers(Modifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public TerminalKey build() {
            return new TerminalKey(input, modifiers);
        }
    }
}

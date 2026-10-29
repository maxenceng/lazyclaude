package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.KeyInput;
import dev.maxenceng.lazyclaude.session.published.Modifiers;

/** One key press with the modifiers held. */
public record EmulatorKeyPayload(KeyInput input, Modifiers modifiers) implements EmulatorInputPayload {

    public EmulatorKeyPayload {
        Assert.notNull("key input", input);
        Assert.notNull("modifiers", modifiers);
    }

    public static EmulatorKeyPayloadBuilder builder() {
        return new EmulatorKeyPayloadBuilder();
    }

    public static final class EmulatorKeyPayloadBuilder {
        private KeyInput input;
        private Modifiers modifiers = Modifiers.none();

        private EmulatorKeyPayloadBuilder() {}

        public EmulatorKeyPayloadBuilder input(KeyInput input) {
            this.input = input;
            return this;
        }

        public EmulatorKeyPayloadBuilder modifiers(Modifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public EmulatorKeyPayload build() {
            return new EmulatorKeyPayload(input, modifiers);
        }
    }
}

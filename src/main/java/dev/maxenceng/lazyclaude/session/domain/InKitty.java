package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.ResidenceName;

/** The session runs in a kitty window: the kitty process that owns it and the window's id, from {@code KITTY_PID} and {@code KITTY_WINDOW_ID}. */
public record InKitty(ProcessId kitty, KittyWindowId window) implements Residence {

    private static final ResidenceName NAME = new ResidenceName("kitty");

    public InKitty {
        Assert.notNull("kitty", kitty);
        Assert.notNull("kitty window", window);
    }

    @Override
    public Optional<ResidenceName> name() {
        return Optional.of(NAME);
    }

    public static InKittyBuilder builder() {
        return new InKittyBuilder();
    }

    public static final class InKittyBuilder {
        private ProcessId kitty;
        private KittyWindowId window;

        private InKittyBuilder() {}

        public InKittyBuilder kitty(ProcessId kitty) {
            this.kitty = kitty;
            return this;
        }

        public InKittyBuilder window(KittyWindowId window) {
            this.window = window;
            return this;
        }

        public InKitty build() {
            return new InKitty(kitty, window);
        }
    }
}

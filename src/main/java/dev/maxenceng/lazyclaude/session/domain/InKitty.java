package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** The session runs in a kitty window: the kitty process that owns it and the window's id, from {@code KITTY_PID} and {@code KITTY_WINDOW_ID}. */
public record InKitty(ProcessId kitty, KittyWindowId window) implements Residence {

    public InKitty {
        Assert.notNull("kitty", kitty);
        Assert.notNull("kitty window", window);
    }

    /** The socket the kitty that owns the window listens on, once {@code kitty.conf} has the lines lazyclaude offers. */
    public KittySocket socket() {
        return KittySettings.socketOf(kitty);
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

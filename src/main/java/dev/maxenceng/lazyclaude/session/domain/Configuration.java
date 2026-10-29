package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

/** What the user may set: the chords reserved inside a hosted session, and whether a waiting session shows a desktop notification, and whether the terminal's mouse is captured. */
public record Configuration(ReservedChords reservedChords, boolean desktopNotifications, boolean mouse) {

    public Configuration {
        Assert.notNull("reserved chords", reservedChords);
    }

    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }

    public static final class ConfigurationBuilder {
        private ReservedChords reservedChords;
        private Boolean desktopNotifications;
        private Boolean mouse;

        private ConfigurationBuilder() {}

        public ConfigurationBuilder reservedChords(ReservedChords reservedChords) {
            this.reservedChords = reservedChords;
            return this;
        }

        public ConfigurationBuilder desktopNotifications(boolean desktopNotifications) {
            this.desktopNotifications = desktopNotifications;
            return this;
        }

        public ConfigurationBuilder mouse(boolean mouse) {
            this.mouse = mouse;
            return this;
        }

        public Configuration build() {
            Assert.notNull("desktopNotifications", desktopNotifications);
            Assert.notNull("mouse", mouse);
            return new Configuration(reservedChords, desktopNotifications, mouse);
        }
    }
}

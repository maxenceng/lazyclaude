package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

/** What the user may set: the chords reserved inside a hosted session, and whether a waiting session shows a desktop notification. */
public record Configuration(ReservedChords reservedChords, boolean desktopNotifications) {

    public Configuration {
        Assert.notNull("reserved chords", reservedChords);
    }

    /** What a missing file means: the layout decision's chords, and notifications on. */
    public static Configuration defaults() {
        return new Configuration(ReservedChords.defaults(), true);
    }

    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }

    public static final class ConfigurationBuilder {
        private ReservedChords reservedChords;
        private Boolean desktopNotifications;

        private ConfigurationBuilder() {}

        public ConfigurationBuilder reservedChords(ReservedChords reservedChords) {
            this.reservedChords = reservedChords;
            return this;
        }

        public ConfigurationBuilder desktopNotifications(boolean desktopNotifications) {
            this.desktopNotifications = desktopNotifications;
            return this;
        }

        public Configuration build() {
            Assert.notNull("desktopNotifications", desktopNotifications);
            return new Configuration(reservedChords, desktopNotifications);
        }
    }
}

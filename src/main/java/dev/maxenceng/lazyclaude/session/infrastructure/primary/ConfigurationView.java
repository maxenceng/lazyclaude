package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.Configuration;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

/** What the user configured: the chords the shell takes inside a hosted session, and whether a waiting session shows a desktop notification. */
public record ConfigurationView(ReservedChords reservedChords, boolean desktopNotifications) {

    static ConfigurationView from(Configuration configuration) {
        return builder()
                .reservedChords(configuration.reservedChords())
                .desktopNotifications(configuration.desktopNotifications())
                .build();
    }

    public static ConfigurationViewBuilder builder() {
        return new ConfigurationViewBuilder();
    }

    public static final class ConfigurationViewBuilder {
        private ReservedChords reservedChords;
        private Boolean desktopNotifications;

        private ConfigurationViewBuilder() {}

        public ConfigurationViewBuilder reservedChords(ReservedChords reservedChords) {
            this.reservedChords = reservedChords;
            return this;
        }

        public ConfigurationViewBuilder desktopNotifications(boolean desktopNotifications) {
            this.desktopNotifications = desktopNotifications;
            return this;
        }

        public ConfigurationView build() {
            Assert.notNull("desktopNotifications", desktopNotifications);
            return new ConfigurationView(reservedChords, desktopNotifications);
        }
    }
}

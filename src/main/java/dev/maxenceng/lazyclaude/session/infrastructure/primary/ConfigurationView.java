package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.Configuration;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

/** What the user configured: the chords the shell takes inside a hosted session, and whether a waiting session shows a desktop notification, and whether the shell captures the mouse. */
public record ConfigurationView(ReservedChords reservedChords, boolean desktopNotifications, boolean mouse) {

    static ConfigurationView from(Configuration configuration) {
        return builder()
                .reservedChords(configuration.reservedChords())
                .desktopNotifications(configuration.desktopNotifications())
                .mouse(configuration.mouse())
                .build();
    }

    public static ConfigurationViewBuilder builder() {
        return new ConfigurationViewBuilder();
    }

    public static final class ConfigurationViewBuilder {
        private ReservedChords reservedChords;
        private Boolean desktopNotifications;
        private Boolean mouse;

        private ConfigurationViewBuilder() {}

        public ConfigurationViewBuilder reservedChords(ReservedChords reservedChords) {
            this.reservedChords = reservedChords;
            return this;
        }

        public ConfigurationViewBuilder desktopNotifications(boolean desktopNotifications) {
            this.desktopNotifications = desktopNotifications;
            return this;
        }

        public ConfigurationViewBuilder mouse(boolean mouse) {
            this.mouse = mouse;
            return this;
        }

        public ConfigurationView build() {
            Assert.notNull("desktopNotifications", desktopNotifications);
            Assert.notNull("mouse", mouse);
            return new ConfigurationView(reservedChords, desktopNotifications, mouse);
        }
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.rebindingFocusLeft;

import dev.maxenceng.lazyclaude.session.published.ReservedChords;

public final class ConfigurationViewFixture {

    private ConfigurationViewFixture() {}

    public static ConfigurationView defaultConfigurationView() {
        return ConfigurationView.builder()
                .reservedChords(ReservedChords.defaults())
                .desktopNotifications(true)
                .build();
    }

    public static ConfigurationView reboundConfigurationView() {
        return ConfigurationView.builder()
                .reservedChords(rebindingFocusLeft().reservedChords())
                .desktopNotifications(true)
                .build();
    }
}

package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.defaultConfigurationView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.mouselessConfigurationView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.reboundConfigurationView;
import static dev.maxenceng.lazyclaude.sidecar.ConfigurationSidecarFixture.defaultConfiguration;
import static dev.maxenceng.lazyclaude.sidecar.ConfigurationSidecarFixture.mouselessConfiguration;
import static dev.maxenceng.lazyclaude.sidecar.ConfigurationSidecarFixture.reboundConfiguration;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigurationSidecarTest {

    @Test
    void answers_the_default_chords_with_their_modifiers_and_keys() {
        assertThat(linesWritten(defaultConfigurationView(), request(1, "session/configuration", "{}")))
                .containsExactly(ready(), resultOf(1, defaultConfiguration()));
    }

    @Test
    void answers_a_rebound_chord_in_place_of_its_default() {
        assertThat(linesWritten(reboundConfigurationView(), request(1, "session/configuration", "{}")))
                .containsExactly(ready(), resultOf(1, reboundConfiguration()));
    }

    @Test
    void answers_that_the_mouse_is_not_captured() {
        assertThat(linesWritten(mouselessConfigurationView(), request(1, "session/configuration", "{}")))
                .containsExactly(ready(), resultOf(1, mouselessConfiguration()));
    }
}

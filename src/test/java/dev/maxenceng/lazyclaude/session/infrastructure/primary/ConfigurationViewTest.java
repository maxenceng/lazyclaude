package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.rebindingFocusLeft;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.reboundConfigurationView;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigurationViewTest {

    @Test
    void holds_the_reserved_chords_it_was_built_from() {
        assertThat(reboundConfigurationView().reservedChords()).isEqualTo(rebindingFocusLeft().reservedChords());
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.rebindingFocusLeft;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.reboundConfigurationView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.ReservedAction;

class ConfigurationViewTest {

    @Test
    void holds_the_reserved_chords_it_was_built_from() {
        assertThat(reboundConfigurationView().reservedChords()).isEqualTo(rebindingFocusLeft().reservedChords());
    }

    @Test
    void holds_one_chord_per_action_in_their_order_whatever_list_it_is_given() {
        ReservedChordView zoom = reboundConfigurationView().chords().get(ReservedAction.ZOOM.ordinal());

        ConfigurationView view = new ConfigurationView(List.of(zoom), true, true);

        assertThat(view.chords())
                .as("an action the list leaves out keeps its default, as config.yml's does")
                .extracting(ReservedChordView::action)
                .containsExactly(ReservedAction.values());
    }

    @Test
    void refuses_chords_that_name_one_action_twice() {
        ReservedChordView zoom = reboundConfigurationView().chords().get(ReservedAction.ZOOM.ordinal());

        assertThatThrownBy(() -> new ConfigurationView(List.of(zoom, zoom), true, true))
                .isInstanceOf(ReservedActionTwiceException.class)
                .hasMessageContaining(ReservedAction.ZOOM.configurationName());
    }
}

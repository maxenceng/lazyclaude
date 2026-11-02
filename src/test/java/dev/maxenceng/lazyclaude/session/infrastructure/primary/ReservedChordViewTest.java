package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.AssertionException;

class ReservedChordViewTest {

    @Test
    void refuses_a_chord_without_an_action() {
        assertThatThrownBy(() -> ReservedChordView.builder().chord(altH()).build()).isInstanceOf(AssertionException.class);
    }
}

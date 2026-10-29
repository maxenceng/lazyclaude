package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.shell.DiffDrawnFixture.headerThenTwoLinesThenAThread;
import static dev.maxenceng.lazyclaude.shell.DiffDrawnFixture.headersOnly;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DiffDrawnTest {

    @Test
    void shouldAnswerTheLineDrawnOnARowAndNoneOnAHeaderOrBelowTheLastRow() {
        assertThat(headerThenTwoLinesThenAThread().lineAt(6)).contains(7);
        assertThat(headerThenTwoLinesThenAThread().lineAt(5)).isEmpty();
        assertThat(headerThenTwoLinesThenAThread().lineAt(9)).isEmpty();
        assertThat(headerThenTwoLinesThenAThread().lineAt(4)).isEmpty();
    }

    @Test
    void shouldClampAPointAboveOrBelowTheDiffToTheNearestLineDrawn() {
        assertThat(headerThenTwoLinesThenAThread().nearestLineAt(0)).contains(7);
        assertThat(headerThenTwoLinesThenAThread().nearestLineAt(40)).contains(8);
    }

    @Test
    void shouldAnswerNoLineWhenNoneIsDrawn() {
        assertThat(headersOnly().nearestLineAt(5)).isEqualTo(Optional.empty());
    }
}

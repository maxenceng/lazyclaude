package dev.maxenceng.lazyclaude.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;

class RowsDrawnTest {

    private final RowsDrawn rows = new RowsDrawn(new Rect(4, 10, 20, 6), 7, 3);

    @Test
    void shouldNameTheFirstRowDrawnOnTheFirstLine() {
        assertThat(rows.rowAt(10)).isEqualTo(Optional.of(7));
    }

    @Test
    void shouldNameTheLastRowDrawnOnTheLastLineDrawn() {
        assertThat(rows.rowAt(12)).isEqualTo(Optional.of(9));
    }

    @Test
    void shouldNameNoRowOnTheLinesBelowTheLastRowDrawn() {
        assertThat(rows.rowAt(13)).isEmpty();
    }
}

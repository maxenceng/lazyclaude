package dev.maxenceng.lazyclaude.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;

class HitMapTest {

    private final HitMap hits = new HitMap();

    @Test
    void shouldAnswerTheHitDrawnLastWhereTwoOverlap() {
        PaneHit pane = new PaneHit(Focus.BOARD, new Rect(0, 0, 10, 10));
        RowsHit rows = new RowsHit(Focus.BOARD, new RowsDrawn(new Rect(0, 0, 10, 5), 0, 5));
        hits.add(pane);
        hits.add(rows);

        assertThat(hits.at(3, 3)).isEqualTo(Optional.of(rows));
        assertThat(hits.at(3, 7)).isEqualTo(Optional.of(pane));
    }

    @Test
    void shouldAnswerNothingOutsideEveryHit() {
        hits.add(new PaneHit(Focus.BOARD, new Rect(2, 2, 4, 4)));

        assertThat(hits.at(1, 3)).isEmpty();
        assertThat(hits.at(6, 3)).isEmpty();
        assertThat(hits.at(3, 6)).isEmpty();
    }

    @Test
    void shouldAnswerNothingOnceTheFrameClearedIt() {
        hits.add(new PaneHit(Focus.BOARD, new Rect(0, 0, 4, 4)));
        hits.pickerDrawn(new RowsDrawn(new Rect(0, 0, 4, 4), 0, 2));

        hits.clear();

        assertThat(hits.at(1, 1)).isEmpty();
        assertThat(hits.pickerRowAt(1, 1)).isEmpty();
    }

    @Test
    void shouldNameThePickerRowUnderThePointOnly() {
        hits.pickerDrawn(new RowsDrawn(new Rect(5, 5, 8, 4), 0, 2));

        assertThat(hits.pickerRowAt(6, 6)).isEqualTo(Optional.of(1));
        assertThat(hits.pickerRowAt(6, 7)).isEmpty();
        assertThat(hits.pickerRowAt(4, 5)).isEmpty();
    }
}

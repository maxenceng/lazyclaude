package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.shell.RowLineFixture.rowLinesNumbered;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoreBelowElementTest {

    @Test
    void shouldShowEveryRowAndNoMoreLineWhenTheyAllFit() {
        Screen screen = Screen.of(new MoreBelowElement(rowLinesNumbered(3), 0, true), 12, 3);

        assertThat(screen.text()).contains("row 0", "row 2").doesNotContain("more");
    }

    @Test
    void shouldReplaceTheLastLineWithHowManyRowsRemainBelow() {
        Screen screen = Screen.of(new MoreBelowElement(rowLinesNumbered(10), 0, true), 12, 4);

        assertThat(screen.lines()).containsExactly("row 0       ", "row 1       ", "row 2       ", "↓ 7 more    ");
    }

    @Test
    void shouldScrollToKeepTheSelectedRowAboveTheMoreLine() {
        Screen screen = Screen.of(new MoreBelowElement(rowLinesNumbered(10), 5, true), 12, 4);

        assertThat(screen.lines()).containsExactly("row 3       ", "row 4       ", "row 5       ", "↓ 4 more    ");
    }

    @Test
    void shouldUseTheWholeHeightOnceTheEndIsInView() {
        Screen screen = Screen.of(new MoreBelowElement(rowLinesNumbered(10), 9, true), 12, 4);

        assertThat(screen.lines()).containsExactly("row 6       ", "row 7       ", "row 8       ", "row 9       ");
    }

    @Test
    void shouldShowOnlyRowsWhenThereIsRoomForOneLine() {
        Screen screen = Screen.of(new MoreBelowElement(rowLinesNumbered(10), 0, true), 12, 1);

        assertThat(screen.text()).contains("row 0").doesNotContain("more");
    }

    @Test
    void shouldDrawTheMoreLineDim() {
        Screen screen = Screen.of(new MoreBelowElement(rowLinesNumbered(10), 0, true), 12, 4);

        assertThat(screen.dimText()).contains("↓ 7 more");
    }
}

package dev.maxenceng.lazyclaude.shell;

import java.util.List;

import dev.tamboui.style.Style;

/**
 * One line of a panel as {@link RowsElement} draws it: a gutter and the text, each in a style of its own, then a column of cells at the
 * right edge, which the text is cut short for.
 */
record RowLine(String gutter, Style gutterStyle, String text, Style textStyle, List<RowCell> column) {

    private static final String NO_GUTTER = "";

    static RowLine plain(String text) {
        return ungutteredOf(text, Style.EMPTY, List.of());
    }

    /** A line with nothing in its gutter. */
    static RowLine ungutteredOf(String text, Style textStyle, List<RowCell> column) {
        return new RowLine(NO_GUTTER, Style.EMPTY, text, textStyle, column);
    }

    String whole() {
        return gutter + text;
    }
}

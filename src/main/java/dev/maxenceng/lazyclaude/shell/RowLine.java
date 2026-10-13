package dev.maxenceng.lazyclaude.shell;

import java.util.List;

import dev.tamboui.style.Style;

/**
 * One line of a panel as {@link RowsElement} draws it: a gutter in a style of its own, then the text, then a column of cells at the
 * right edge, which the text is cut short for.
 */
record RowLine(String gutter, Style gutterStyle, String text, List<RowCell> column) {

    static RowLine plain(String text) {
        return new RowLine("", Style.EMPTY, text, List.of());
    }

    String whole() {
        return gutter + text;
    }
}

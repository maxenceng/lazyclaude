package dev.maxenceng.lazyclaude.shell;

import java.util.List;

import dev.tamboui.style.Style;

/**
 * One line of a panel as {@link RowsElement} draws it: a gutter and the text, each in a style of its own, then a column of cells at the
 * right edge, which the text is cut short for.
 */
record RowLine(String gutter, Style gutterStyle, String text, Style textStyle, List<RowCell> column) {

    static RowLine plain(String text) {
        return new RowLine("", Style.EMPTY, text, Style.EMPTY, List.of());
    }

    String whole() {
        return gutter + text;
    }
}

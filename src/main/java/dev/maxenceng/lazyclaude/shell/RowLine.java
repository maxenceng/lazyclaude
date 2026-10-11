package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.style.Style;

/** One line of a panel as {@link RowsElement} draws it: a gutter in a style of its own, then the text. */
record RowLine(String gutter, Style gutterStyle, String text) {

    static RowLine plain(String text) {
        return new RowLine("", Style.EMPTY, text);
    }

    String whole() {
        return gutter + text;
    }
}

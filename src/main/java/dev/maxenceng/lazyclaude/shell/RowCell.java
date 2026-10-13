package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.style.Style;

/** One cell of a row's right-hand column: a glyph in a style of its own, or a blank. */
record RowCell(String glyph, Style style) {

    private static final String BLANK = " ";

    static RowCell blank() {
        return new RowCell(BLANK, Style.EMPTY);
    }
}

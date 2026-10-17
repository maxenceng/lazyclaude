package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.style.Style;

/** A piece of the status line's left side, in a style of its own. */
record StatusSpan(String text, Style style) {

    int width() {
        return text.codePointCount(0, text.length());
    }

    /** The first {@code cells} cells of the text. */
    StatusSpan clipped(int cells) {
        return new StatusSpan(text.substring(0, text.offsetByCodePoints(0, Math.min(cells, width()))), style);
    }
}

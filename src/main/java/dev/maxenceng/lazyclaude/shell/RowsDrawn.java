package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;

import dev.tamboui.layout.Rect;

/** Where a list drew its rows: the cells it was given, the index of the first row drawn and how many were drawn. */
record RowsDrawn(Rect area, int first, int count) {

    /** The index of the row drawn on line {@code y}, none on a line below the last one drawn. */
    Optional<Integer> rowAt(int y) {
        int drawn = y - area.y();
        return drawn < count ? Optional.of(first + drawn) : Optional.empty();
    }
}

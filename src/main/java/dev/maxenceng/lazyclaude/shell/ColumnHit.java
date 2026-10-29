package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.layout.Rect;

/** The rows of one status column that shares the width, counted from the first column. */
record ColumnHit(int column, RowsDrawn rows) implements Hit {

    @Override
    public Rect area() {
        return rows.area();
    }
}

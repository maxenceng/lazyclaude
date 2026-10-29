package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.layout.Rect;

/** The rows of one of the three left panels. */
record RowsHit(Focus pane, RowsDrawn rows) implements Hit {

    @Override
    public Rect area() {
        return rows.area();
    }
}

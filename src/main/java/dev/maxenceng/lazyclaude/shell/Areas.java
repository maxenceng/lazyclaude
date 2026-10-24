package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.layout.Rect;

/** What the shell asks of an area it is given to draw into. */
final class Areas {

    private static final int NO_CELLS = 0;

    private Areas() {}

    /** Whether the area has no cell to draw in: no width or no height, a negative one included. */
    static boolean hasNoRoom(Rect area) {
        return area.width() <= NO_CELLS || area.height() <= NO_CELLS;
    }

    /** Whether {@code cells} leaves no room. */
    static boolean noRoomIn(int cells) {
        return cells <= NO_CELLS;
    }
}

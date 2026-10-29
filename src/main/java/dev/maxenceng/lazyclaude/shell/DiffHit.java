package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.layout.Rect;

/** The diff of an open review, where the mouse reaches its lines. */
record DiffHit(DiffDrawn drawn) implements Hit {

    @Override
    public Rect area() {
        return drawn.area();
    }
}

package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.layout.Rect;

/** What the last frame drew somewhere on screen that the mouse reaches. */
sealed interface Hit permits RowsHit, ColumnHit, PaneHit, TabHit {

    Rect area();
}

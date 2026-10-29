package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.layout.Rect;

/** A pane that takes focus when it is clicked anywhere inside. */
record PaneHit(Focus pane, Rect area) implements Hit {}

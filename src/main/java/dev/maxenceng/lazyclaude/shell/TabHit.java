package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.layout.Rect;

/** The label of one dock tab, in the dock's title or in the strip it folds to. */
record TabHit(int index, Rect area) implements Hit {}

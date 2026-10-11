package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.style.Style;

/** What sits between a session row's glyph and its age: an optional verb, the title, and an optional dim suffix. */
record MiddleText(String verb, Style verbStyle, String title, Style titleStyle, String suffix) {}

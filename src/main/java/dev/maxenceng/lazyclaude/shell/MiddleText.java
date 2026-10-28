package dev.maxenceng.lazyclaude.shell;

import dev.tamboui.style.Style;

/** What sits between a session row's glyph and its age: an optional verb and the title. */
record MiddleText(String verb, Style verbStyle, String title, Style titleStyle) {}

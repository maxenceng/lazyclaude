package dev.maxenceng.lazyclaude.session.published;

/** Where the session's cursor is, or that it is hidden. */
public sealed interface ScreenCursor permits CursorAt, NoCursor {}

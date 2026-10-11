package dev.maxenceng.lazyclaude.session.published;

/** What the shell hands a hosted session: a key, a paste, or a mouse event, in lazyclaude's types and not the screen library's. */
public sealed interface TerminalInput permits TerminalKey, PastedText, TerminalMouse {}

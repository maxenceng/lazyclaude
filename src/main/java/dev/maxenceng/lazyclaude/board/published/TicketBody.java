package dev.maxenceng.lazyclaude.board.published;

/** An issue's body: markdown text, or nothing when it is empty or only whitespace. */
public sealed interface TicketBody permits Body, NoBody {}

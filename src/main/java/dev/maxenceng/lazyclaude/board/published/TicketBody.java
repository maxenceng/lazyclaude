package dev.maxenceng.lazyclaude.board.published;

/** What follows a ticket's frontmatter: markdown text, or nothing when it is empty or only whitespace. */
public sealed interface TicketBody permits Body, NoBody {}

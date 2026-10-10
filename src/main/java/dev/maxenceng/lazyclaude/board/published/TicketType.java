package dev.maxenceng.lazyclaude.board.published;

/** The kind of work a ticket's frontmatter names, or none when it names none. */
public sealed interface TicketType permits TypeName, NoType {}

package dev.maxenceng.lazyclaude.board.published;

/** The kind of work a ticket's `type:` label names, or none when it has none. */
public sealed interface TicketType permits TypeName, NoType {}

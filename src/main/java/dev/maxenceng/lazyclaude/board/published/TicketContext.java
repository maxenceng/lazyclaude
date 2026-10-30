package dev.maxenceng.lazyclaude.board.published;

/** The bounded context a ticket's `context:` label names, or none when it has none. */
public sealed interface TicketContext permits ContextName, NoContext {}

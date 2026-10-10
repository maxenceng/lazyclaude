package dev.maxenceng.lazyclaude.board.published;

/** The bounded context a ticket's frontmatter names, or none when it names none. */
public sealed interface TicketContext permits ContextName, NoContext {}

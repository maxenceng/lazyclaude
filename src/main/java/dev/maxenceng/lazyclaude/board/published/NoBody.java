package dev.maxenceng.lazyclaude.board.published;

/** The ticket has nothing after its frontmatter; it is still a ticket, not an unreadable one. */
public record NoBody() implements TicketBody {}

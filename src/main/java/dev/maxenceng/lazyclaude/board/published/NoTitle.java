package dev.maxenceng.lazyclaude.board.published;

/** The ticket has no `# ` heading; it is still a ticket, not an unreadable one. */
public record NoTitle() implements TicketTitle {}

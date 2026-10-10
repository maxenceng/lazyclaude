package dev.maxenceng.lazyclaude.board.published;

/** A backlog file that could not be read as a ticket, and why. */
public record UnreadableTicketView(String file, String reason) {}

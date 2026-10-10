package dev.maxenceng.lazyclaude.board.domain;

/** One backlog file that claims to be a ticket: read into one, or refused with a reason. */
public sealed interface BacklogEntry permits TicketFile, UnreadableTicket {

    String file();
}

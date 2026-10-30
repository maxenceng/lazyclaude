package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketId;

/** One issue that claims to be a ticket: read into one, or refused with a reason. */
public sealed interface BacklogEntry permits Ticket, UnreadableTicket {

    TicketId id();
}

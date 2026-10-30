package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketId;

/** One ticket as stored on GitHub: read, or refused as unreadable with a reason. */
public sealed interface BacklogEntry permits Ticket, UnreadableTicket {

    TicketId id();
}

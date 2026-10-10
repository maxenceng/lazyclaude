package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketTitle;
import dev.maxenceng.lazyclaude.board.published.TicketType;

/** One ticket as the shell reads it. */
public record TicketView(TicketId id, TicketTitle title, Status status, TicketContext context, TicketType type) {

    static TicketView from(Ticket ticket) {
        return new TicketView(ticket.id(), ticket.title(), ticket.status(), ticket.context(), ticket.type());
    }
}

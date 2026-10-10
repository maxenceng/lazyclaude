package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Reason;

/** A backlog file that could not be read as a ticket, and why. */
public record UnreadableTicketView(FileName file, Reason reason) {

    static UnreadableTicketView from(UnreadableTicket unreadable) {
        return new UnreadableTicketView(unreadable.file(), unreadable.reason());
    }
}

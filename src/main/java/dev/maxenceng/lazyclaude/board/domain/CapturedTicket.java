package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** GitHub took the ticket and numbered it; the board does not show it until it is read again. */
public record CapturedTicket(TicketId id) implements TicketCapture {

    public CapturedTicket {
        Assert.notNull("id", id);
    }
}

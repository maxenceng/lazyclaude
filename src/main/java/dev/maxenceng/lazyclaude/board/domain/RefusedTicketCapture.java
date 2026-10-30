package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The ticket was not written, or not known to be; the reason says why, and whether to try again is the person's to decide. */
public record RefusedTicketCapture(Reason reason) implements TicketCapture {

    public RefusedTicketCapture {
        Assert.notNull("reason", reason);
    }
}

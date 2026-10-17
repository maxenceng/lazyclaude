package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** Told when a hosted session started to run {@code /ticket done} on a ticket has ended, on whichever thread ended it. */
@FunctionalInterface
public interface TicketDoneEndedListener {

    void ended(LinkedTicketId ticket);
}

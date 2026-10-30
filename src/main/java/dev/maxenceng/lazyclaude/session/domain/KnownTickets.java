package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** The ids of the tickets on the board, as the shell hands them over; the only thing a session is linked against. */
public record KnownTickets(List<LinkedTicketId> ids) {

    public KnownTickets {
        Assert.notNull("ticket ids", ids);
    }

    /** The board's id that {@code named} names. */
    Optional<LinkedTicketId> find(NamedTicketId named) {
        return ids.stream().filter(id -> id.value().equals(named.value())).findFirst();
    }
}

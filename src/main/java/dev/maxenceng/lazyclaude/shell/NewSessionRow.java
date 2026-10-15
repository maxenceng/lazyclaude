package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** The last row of a ticket's section: {@code n new: /ticket <verb>}, which {@code enter} opens the verb picker from. */
record NewSessionRow(LinkedTicketId ticket, Verb verb) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.NEW_SESSION, ticket.value());
    }
}

package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** What a ticket's section holds when no session is linked to it. */
record NoSessionRow(LinkedTicketId ticket) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.NO_SESSION, ticket.value());
    }
}

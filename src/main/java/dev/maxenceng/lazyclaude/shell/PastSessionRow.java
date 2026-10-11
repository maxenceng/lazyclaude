package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/** A past session, with how it is linked to the ticket whose section lists it, if it is. It is keyed like a live one, so the selection stays on a session whose process died. */
record PastSessionRow(PastSessionView session, Optional<TicketLink> link) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.SESSION, session.id().value());
    }
}

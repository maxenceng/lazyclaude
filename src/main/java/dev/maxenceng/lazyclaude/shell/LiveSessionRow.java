package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/** A live session, with how it is linked to the ticket whose section lists it, if it is. */
record LiveSessionRow(LiveSessionView session, Optional<TicketLink> link) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.SESSION, session.id().value());
    }
}

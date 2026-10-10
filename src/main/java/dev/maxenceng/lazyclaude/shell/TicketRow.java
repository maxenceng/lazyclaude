package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.published.TicketView;

/** A ticket under its status's header: id, then title. */
record TicketRow(TicketView ticket) implements Row {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.TICKET, ticket.id());
    }

    @Override
    public String text() {
        return "%s%s %s".formatted(INDENT, ticket.id(), ticket.title());
    }
}

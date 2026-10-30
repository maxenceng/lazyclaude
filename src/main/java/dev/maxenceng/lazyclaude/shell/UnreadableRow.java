package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketView;

/** An issue that could not be read as a ticket, under the unreadable header: number, then reason. */
record UnreadableRow(UnreadableTicketView unreadable) implements Row {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.UNREADABLE, unreadable.id().value());
    }

    @Override
    public String text() {
        return "%s%s — %s".formatted(Indent.STEP, TicketText.number(unreadable.id()), unreadable.reason().value());
    }
}

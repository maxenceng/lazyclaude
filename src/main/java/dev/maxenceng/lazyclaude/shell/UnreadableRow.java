package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketView;

/** A backlog file that could not be read, under the unreadable header: file, then reason. */
record UnreadableRow(UnreadableTicketView unreadable) implements Row {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.UNREADABLE, unreadable.file().value());
    }

    @Override
    public String text() {
        return "%s%s — %s".formatted(Indent.STEP, unreadable.file().value(), unreadable.reason().value());
    }
}

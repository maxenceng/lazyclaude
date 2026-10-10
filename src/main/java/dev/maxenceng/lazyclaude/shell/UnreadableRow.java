package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.published.UnreadableTicketView;

/** A backlog file that could not be read, under the unreadable header: file, then reason. */
record UnreadableRow(UnreadableTicketView unreadable) implements Row {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.UNREADABLE, unreadable.file());
    }

    @Override
    public String text() {
        return "%s%s — %s".formatted(INDENT, unreadable.file(), unreadable.reason());
    }
}

package dev.maxenceng.lazyclaude.shell;

/** One line of the Board panel. */
sealed interface Row permits HeaderRow, TicketRow, UnavailableRow, UnreadableRow {

    RowKey key();

    String text();
}

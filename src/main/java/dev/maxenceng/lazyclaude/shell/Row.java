package dev.maxenceng.lazyclaude.shell;

/** One line of the Board panel. */
sealed interface Row permits HeaderRow, TicketRow, UnreadableRow {

    /** Rows under a group header are indented by this much. */
    String INDENT = "  ";

    RowKey key();

    String text();
}

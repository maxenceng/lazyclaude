package dev.maxenceng.lazyclaude.shell;

/** One line of the Sessions panel. */
sealed interface SessionRow permits LiveSessionRow,
                PastSessionRow,
                OlderSessionsRow,
                SectionHeaderRow,
                NoSessionRow,
                NewSessionRow,
                StartingSessionRow {

    RowKey key();

    /** Whether the cursor can stand on the row: a header is only a label. */
    default boolean selectable() {
        return true;
    }
}

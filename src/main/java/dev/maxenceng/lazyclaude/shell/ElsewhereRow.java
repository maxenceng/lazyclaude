package dev.maxenceng.lazyclaude.shell;

/** One line of the Elsewhere panel. */
sealed interface ElsewhereRow permits ElsewhereHeaderRow, ElsewhereSessionRow {

    RowKey key();

    /** Whether the cursor can stand on the row: a header is only a label. */
    boolean selectable();

    RowLine line();
}

package dev.maxenceng.lazyclaude.shell;

/** One line of the Docs panel. */
sealed interface DocsRow permits FolderRow, DocumentRow {

    /** Rows inside a folder are indented by this much per folder. */
    String INDENT = Row.INDENT;

    RowKey key();

    String text();
}

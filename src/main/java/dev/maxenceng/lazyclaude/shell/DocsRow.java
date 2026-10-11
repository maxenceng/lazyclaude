package dev.maxenceng.lazyclaude.shell;

/** One line of the Docs panel. */
sealed interface DocsRow permits FolderRow, DocumentRow {

    RowKey key();

    String text();
}

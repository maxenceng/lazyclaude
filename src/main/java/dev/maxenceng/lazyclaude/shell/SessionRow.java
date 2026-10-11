package dev.maxenceng.lazyclaude.shell;

/** One line of the Sessions panel. */
sealed interface SessionRow permits LiveSessionRow, PastSessionRow, OlderSessionsRow {

    RowKey key();
}

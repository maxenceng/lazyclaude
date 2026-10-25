package dev.maxenceng.lazyclaude.shell;

/** Where a ticket stands among the status columns that share the width: which column, and which row of it. Computed, never kept. */
record StatusColumnsPlace(int column, int row) {}

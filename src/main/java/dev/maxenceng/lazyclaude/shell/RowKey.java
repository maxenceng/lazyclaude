package dev.maxenceng.lazyclaude.shell;

/** What the selection and the folded groups remember a row by, so they survive the rows being rebuilt. */
record RowKey(RowKind kind, String name) {}

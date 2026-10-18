package dev.maxenceng.lazyclaude.shell;

/** One row of the review's main view: a file header, a hunk header, a diff line, or a thread shown under the line it sits on. */
sealed interface DiffRow permits FileHeaderRow, HunkHeaderRow, LineRow, ThreadRow {}

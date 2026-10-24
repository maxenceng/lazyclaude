package dev.maxenceng.lazyclaude.shell;

/** One row of the Comments panel: a pending comment or a thread. */
sealed interface CommentEntry permits PendingEntry, ThreadEntry {}

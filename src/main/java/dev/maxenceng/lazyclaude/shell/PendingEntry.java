package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingInDiffView;

/** A pending comment in the Comments panel, with the diff line it shows under. */
record PendingEntry(PendingInDiffView placed) implements CommentEntry {}

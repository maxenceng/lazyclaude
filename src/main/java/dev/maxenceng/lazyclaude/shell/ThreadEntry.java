package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadInDiffView;

/** A thread in the Comments panel, with the diff line it shows under. */
record ThreadEntry(ThreadInDiffView placed) implements CommentEntry {}

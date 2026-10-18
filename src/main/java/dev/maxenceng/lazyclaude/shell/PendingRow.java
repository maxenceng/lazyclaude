package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentView;

/** A pending comment drawn under the line it sits on, over as many screen rows as its text takes. */
record PendingRow(PendingCommentView comment) implements DiffRow {}

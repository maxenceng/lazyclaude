package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentView;

/** The text of a pending comment, which saving it empty deletes. */
record EditCommentTarget(PendingCommentView comment) implements BoxTarget {}

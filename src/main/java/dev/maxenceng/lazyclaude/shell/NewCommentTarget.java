package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.OnDiffAnchorView;

/** A new comment on the lines of the diff that {@code anchor} says. */
record NewCommentTarget(OnDiffAnchorView anchor) implements BoxTarget {}

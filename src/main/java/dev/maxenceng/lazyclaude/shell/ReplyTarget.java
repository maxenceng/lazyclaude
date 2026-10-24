package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;

/** A reply to {@code thread}. */
record ReplyTarget(ReviewThreadView thread) implements BoxTarget {}

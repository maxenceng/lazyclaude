package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.Reason;

/** The lines asked for take no comment, and why; the range they were asked for is left as it was. */
public record RefusedAnchorView(Reason reason) implements CommentAnchorView {}

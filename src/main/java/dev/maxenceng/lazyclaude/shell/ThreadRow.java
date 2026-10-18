package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;

/** A thread drawn under the line it sits on, over as many screen rows as its comments take. */
record ThreadRow(ReviewThreadView thread) implements DiffRow {}

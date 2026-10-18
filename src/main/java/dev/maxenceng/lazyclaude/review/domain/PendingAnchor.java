package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

/** What a pending comment hangs on: lines of the diff, or an existing thread it answers, which sits on lines of its own. */
public sealed interface PendingAnchor permits PendingOnDiff, PendingReply {

    DiffPath path();

    ThreadPosition position();
}

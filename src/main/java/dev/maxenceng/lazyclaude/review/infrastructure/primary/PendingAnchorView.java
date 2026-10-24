package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.PendingAnchor;
import dev.maxenceng.lazyclaude.review.domain.PendingOnDiff;
import dev.maxenceng.lazyclaude.review.domain.PendingReply;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

/** What a pending comment hangs on: lines of the diff, or a thread it answers. */
public sealed interface PendingAnchorView permits OnDiffAnchorView, ReplyAnchorView {

    DiffPath path();

    ThreadPosition position();

    PendingAnchor toDomain();

    static PendingAnchorView from(PendingAnchor anchor) {
        return switch (anchor) {
            case PendingOnDiff onDiff -> OnDiffAnchorView.from(onDiff);
            case PendingReply reply -> ReplyAnchorView.from(reply);
        };
    }
}

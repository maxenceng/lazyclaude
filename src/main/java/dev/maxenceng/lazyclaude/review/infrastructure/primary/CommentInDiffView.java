package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.CommentInDiff;
import dev.maxenceng.lazyclaude.review.domain.PendingInDiff;
import dev.maxenceng.lazyclaude.review.domain.ThreadInDiff;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/** One entry of Comments with the diff line it shows under: a thread, or one of your pending comments. */
public sealed interface CommentInDiffView permits ThreadInDiffView, PendingInDiffView {

    ThreadLine line();

    static CommentInDiffView from(CommentInDiff listed) {
        return switch (listed) {
            case ThreadInDiff thread -> ThreadInDiffView.from(thread);
            case PendingInDiff pending -> PendingInDiffView.from(pending);
        };
    }
}

package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/** One entry of Comments with the diff line it shows under: a thread, or one of your pending comments. */
public sealed interface CommentInDiff permits ThreadInDiff, PendingInDiff {

    ThreadLine line();
}

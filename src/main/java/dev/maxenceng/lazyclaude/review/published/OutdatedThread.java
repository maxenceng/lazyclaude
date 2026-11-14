package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A thread whose line the pull request no longer has, so it is placed on no line; it knows where it was, when GitHub says. */
public record OutdatedThread(OriginalPlace was) implements ThreadPosition {

    public OutdatedThread {
        Assert.notNull("original place", was);
    }
}

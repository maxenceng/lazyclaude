package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * The thread is on a file the diff holds but on no line of it: an outdated thread, or one whose line the diff does not show; it shows
 * under the rule of the file at {@code file}, counted from 0 in the order the diff lists its files.
 */
public record AtFileHead(int file) implements ThreadLine {

    public AtFileHead {
        Assert.notNegative("file", file);
    }
}

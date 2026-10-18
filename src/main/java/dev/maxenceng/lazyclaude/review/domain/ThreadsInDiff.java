package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The threads in diff order, each with its line: by the line they show under, the ones on no line last, by file and line. */
public record ThreadsInDiff(List<ThreadInDiff> threads) {

    public ThreadsInDiff {
        Assert.notNull("threads", threads);
    }
}

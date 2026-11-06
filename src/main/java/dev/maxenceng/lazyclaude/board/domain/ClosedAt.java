package dev.maxenceng.lazyclaude.board.domain;

import java.time.Instant;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The moment GitHub closed a ticket's issue. */
public record ClosedAt(Instant at) implements Closure {

    public ClosedAt {
        Assert.notNull("at", at);
    }
}

package dev.maxenceng.lazyclaude.session.published;

import java.time.Instant;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** When a session's process started; rows are ordered by it. */
public record StartedAt(Instant value) implements Comparable<StartedAt> {

    public StartedAt {
        Assert.notNull("start time", value);
    }

    @Override
    public int compareTo(StartedAt other) {
        return value.compareTo(other.value);
    }
}

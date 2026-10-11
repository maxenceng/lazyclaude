package dev.maxenceng.lazyclaude.session.domain;

import java.time.Instant;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The timestamp of a transcript's last entry; past sessions are ordered by it. */
public record LastActivity(Instant value) implements Comparable<LastActivity> {

    public LastActivity {
        Assert.notNull("last activity", value);
    }

    @Override
    public int compareTo(LastActivity other) {
        return value.compareTo(other.value);
    }
}

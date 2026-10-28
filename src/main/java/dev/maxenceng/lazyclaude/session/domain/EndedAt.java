package dev.maxenceng.lazyclaude.session.domain;

import java.time.Instant;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** When a session's {@code claude} exited by itself. */
public record EndedAt(Instant value) implements LaunchEnd {

    public EndedAt {
        Assert.notNull("end time", value);
    }
}

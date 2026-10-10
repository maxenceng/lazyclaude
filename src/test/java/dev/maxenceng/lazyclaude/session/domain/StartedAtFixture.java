package dev.maxenceng.lazyclaude.session.domain;

import java.time.Instant;

public final class StartedAtFixture {

    private StartedAtFixture() {}

    public static Instant now() {
        return Instant.parse("2026-10-10T14:00:00Z");
    }

    public static StartedAt startedAt() {
        return new StartedAt(Instant.parse("2026-10-10T12:00:00Z"));
    }

    public static StartedAt earlierStartedAt() {
        return new StartedAt(Instant.parse("2026-10-09T12:00:00Z"));
    }

    public static StartedAt laterStartedAt() {
        return new StartedAt(Instant.parse("2026-10-10T13:48:00Z"));
    }
}

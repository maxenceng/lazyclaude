package dev.maxenceng.lazyclaude.board.domain;

import java.time.Instant;

public final class ClosedAtFixture {

    private ClosedAtFixture() {}

    public static Instant closedTime() {
        return Instant.parse("2026-10-01T09:00:00Z");
    }

    public static Instant laterClosedTime() {
        return Instant.parse("2026-10-02T09:00:00Z");
    }

    public static ClosedAt closedAt() {
        return new ClosedAt(closedTime());
    }

    public static ClosedAt laterClosedAt() {
        return new ClosedAt(laterClosedTime());
    }
}

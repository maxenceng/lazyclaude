package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.startedAt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class SpawnedEarlierClock extends Clock {

    private static final String SPAWNER = HostedSessionManager.class.getName();

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return StackWalker.getInstance().walk(frames -> frames.anyMatch(frame -> frame.getClassName().equals(SPAWNER)))
                ? startedAt().value()
                : now();
    }
}

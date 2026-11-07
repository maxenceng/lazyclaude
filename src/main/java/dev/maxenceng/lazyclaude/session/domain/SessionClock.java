package dev.maxenceng.lazyclaude.session.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionAge;

/** Now, as the composition root's clock says it, so no rule reads the system time on its own. */
public record SessionClock(Clock clock) {

    public SessionClock {
        Assert.notNull("clock", clock);
    }

    /** How long ago {@code local}, a time in this clock's zone, was. */
    public SessionAge ageSince(LocalDateTime local) {
        return new SessionAge(Duration.between(local, LocalDateTime.now(clock)));
    }

    public LocalDateTime localTimeOf(StartedAt startedAt) {
        return LocalDateTime.ofInstant(startedAt.value(), clock.getZone());
    }

    public LocalDateTime localTimeOf(LastActivity lastActivity) {
        return LocalDateTime.ofInstant(lastActivity.value(), clock.getZone());
    }
}

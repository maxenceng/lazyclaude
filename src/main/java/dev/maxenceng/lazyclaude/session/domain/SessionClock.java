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

    /** This clock stopped at the instant it is asked, so every age of one read is measured from the same now. */
    public SessionClock frozen() {
        return new SessionClock(Clock.fixed(clock.instant(), clock.getZone()));
    }

    public SessionAge ageOf(StartedAt startedAt) {
        return new SessionAge(Duration.between(startedAt.value(), clock.instant()));
    }

    public LocalDateTime localTimeOf(StartedAt startedAt) {
        return LocalDateTime.ofInstant(startedAt.value(), clock.getZone());
    }
}

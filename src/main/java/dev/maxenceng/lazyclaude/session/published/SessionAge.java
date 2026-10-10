package dev.maxenceng.lazyclaude.session.published;

import java.time.Duration;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** How long a session has run, shown whole: minutes under an hour, hours under a day, then days. */
public record SessionAge(Duration value) {

    private static final long MINUTES_PER_HOUR = 60;
    private static final long HOURS_PER_DAY = 24;

    public SessionAge {
        Assert.notNull("age", value);
        if (value.isNegative()) {
            value = Duration.ZERO;
        }
    }

    public String label() {
        if (value.toMinutes() < MINUTES_PER_HOUR) {
            return "%dm".formatted(value.toMinutes());
        }
        if (value.toHours() < HOURS_PER_DAY) {
            return "%dh".formatted(value.toHours());
        }
        return "%dd".formatted(value.toDays());
    }
}

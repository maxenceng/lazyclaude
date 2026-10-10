package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionClock;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.startedAt;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.SessionAge;

class SessionClockTest {

    @Test
    void shouldReadAnAgeFromTheInjectedClock() {
        assertThat(sessionClock().ageOf(startedAt())).isEqualTo(new SessionAge(Duration.ofHours(2)));
    }

    @Test
    void shouldReadALocalStartInTheClocksZone() {
        assertThat(sessionClock().localTimeOf(startedAt())).isEqualTo(LocalDateTime.of(2026, 10, 10, 12, 0));
    }
}

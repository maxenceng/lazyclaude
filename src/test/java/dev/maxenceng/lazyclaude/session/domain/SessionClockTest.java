package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.parisClockAfterSummerTimeEnds;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.parisTimeBeforeSummerTimeEnds;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionClock;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.startedAt;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;


class SessionClockTest {

    @Test
    void shouldReadTheAgeOfALocalTimeInTheClocksZone() {
        assertThat(sessionClock().ageSince(LocalDateTime.of(2026, 10, 10, 13, 50))).isEqualTo(new SessionAge(Duration.ofMinutes(10)));
    }

    @Test
    void shouldReadTheTimeElapsedAcrossTheEndOfSummerTime() {
        assertThat(parisClockAfterSummerTimeEnds().ageSince(parisTimeBeforeSummerTimeEnds()))
                .as("01:30 CEST to 02:30 CET is two hours, though the wall clock moved one")
                .isEqualTo(new SessionAge(Duration.ofHours(2)));
    }

    @Test
    void shouldReadALocalStartInTheClocksZone() {
        assertThat(sessionClock().localTimeOf(startedAt())).isEqualTo(LocalDateTime.of(2026, 10, 10, 12, 0));
    }
}

package dev.maxenceng.lazyclaude.session.published;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class SessionAgeTest {

    @Test
    void shouldCountMinutesUpToFiftyNineMinutes() {
        assertThat(new SessionAge(Duration.ofMinutes(59).plusSeconds(59)).label()).isEqualTo("59m");
    }

    @Test
    void shouldTurnToHoursAtSixtyMinutes() {
        assertThat(new SessionAge(Duration.ofMinutes(60)).label()).isEqualTo("1h");
    }

    @Test
    void shouldCountHoursUpToTwentyThreeHours() {
        assertThat(new SessionAge(Duration.ofHours(23).plusMinutes(59)).label()).isEqualTo("23h");
    }

    @Test
    void shouldTurnToDaysAtTwentyFourHours() {
        assertThat(new SessionAge(Duration.ofHours(24)).label()).isEqualTo("1d");
    }

    @Test
    void shouldCountWholeDays() {
        assertThat(new SessionAge(Duration.ofHours(49)).label()).isEqualTo("2d");
    }

    @Test
    void shouldShowZeroMinutesForASessionStartedThisInstant() {
        assertThat(new SessionAge(Duration.ofSeconds(59)).label()).isEqualTo("0m");
    }

    @Test
    void shouldShowZeroMinutesWhenTheStartIsAheadOfTheClock() {
        assertThat(new SessionAge(Duration.ofMinutes(-5)).label())
                .as("a clock behind the registry's must not print a negative age")
                .isEqualTo("0m");
    }
}

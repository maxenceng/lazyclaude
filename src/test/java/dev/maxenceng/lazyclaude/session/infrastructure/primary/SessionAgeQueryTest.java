package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionAgeQueryFixture.sessionAgeQuery;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.SessionAge;

class SessionAgeQueryTest {

    @Test
    void shouldAgeALiveSessionFromItsStart() {
        assertThat(sessionAgeQuery().startedAgo(sessionView())).isEqualTo(new SessionAge(Duration.ofHours(2)));
    }

    @Test
    void shouldAgeAPastSessionFromItsLastActivity() {
        assertThat(sessionAgeQuery().lastActiveAgo(pastSessionView())).isEqualTo(new SessionAge(Duration.ofMinutes(10)));
    }
}

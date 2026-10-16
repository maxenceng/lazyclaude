package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class LastPushedSessionsTest {

    @Test
    void shouldHoldNothingBeforeTheFirstPush() {
        assertThat(new LastPushedSessions().last()).isEmpty();
    }

    @Test
    void shouldGiveBackTheLastPush() {
        LastPushedSessions last = new LastPushedSessions();
        last.remember(unavailableSessions());

        last.remember(liveSessionsOf(List.of()));

        assertThat(last.last()).as("a newer push replaces the one before it").contains(liveSessionsOf(List.of()));
    }
}

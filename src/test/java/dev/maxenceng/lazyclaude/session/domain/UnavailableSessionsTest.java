package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openUnread;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessionsHosting;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class UnavailableSessionsTest {

    @Test
    void shouldKeepTheHostedSessionsOpenAsUnread() {
        Sessions hosting = unavailableSessions().hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting)
                .as("a registry that cannot be read must not close the tabs of claudes that are running")
                .isEqualTo(unavailableSessionsHosting(List.of(openUnread())));
    }
}

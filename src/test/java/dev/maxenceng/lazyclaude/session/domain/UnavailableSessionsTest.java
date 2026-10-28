package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openUnread;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;

class UnavailableSessionsTest {

    @Test
    void shouldKeepTheHostedSessionsOpenAsUnread() {
        Sessions hosting = unavailableSessions().hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting)
                .as("a registry that cannot be read must not close the tabs of claudes that are running")
                .isEqualTo(unavailableSessionsHosting(List.of(openUnread())));
    }

    @Test
    void shouldHaveRunTheVerbAHostedSessionWasLaunchedWithWhateverTheRegistryHolds() {
        Sessions hosting = unavailableSessions().hosting(hostedSessionsOf(hostedSessionRunning(Verb.DESIGN)));

        assertThat(hosting.ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldHaveRunNoVerbWithNoHostedSession() {
        assertThat(unavailableSessions().ranVerb(linkedTicketId(), Verb.DESIGN)).isFalse();
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.ticketClaude;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;

class LaunchCommandTest {

    @Test
    void shouldRunItsVerbOnItsTicketWhenItIsATicketLaunch() {
        assertThat(ticketClaude(Verb.DESIGN).launchRun()).contains(ticketRun("design", "1"));
    }

    @Test
    void shouldRunNothingWhenItBeginsAPlainSession() {
        assertThat(new NewClaude(sessionId()).launchRun()).isEmpty();
    }

    @Test
    void shouldRunNothingWhenItPicksASessionUpAgain() {
        assertThat(new ResumeClaude(sessionId()).launchRun()).isEmpty();
    }
}

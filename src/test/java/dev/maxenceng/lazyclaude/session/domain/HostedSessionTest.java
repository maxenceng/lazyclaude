package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.secondLinkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.SessionName;

class HostedSessionTest {

    @Test
    void shouldBeNamedByTheIdItWasLaunchedWith() {
        assertThat(hostedSession().id()).isEqualTo(sessionId());
    }

    @Test
    void shouldBeNamedByTheIdItResumes() {
        assertThat(hostedSessionBuilder().command(new ResumeClaude(secondSessionId())).build().id()).isEqualTo(secondSessionId());
    }

    @Test
    void shouldBeNamedByTheIdATicketLaunchWasGiven() {
        assertThat(hostedSessionRunning(Verb.REVIEW).id()).isEqualTo(sessionId());
    }

    @Test
    void shouldBeForTheTicketItWasLaunchedFor() {
        assertThat(hostedSessionRunning(Verb.REVIEW).ticket()).contains(linkedTicketId());
    }

    @Test
    void shouldBeForNoTicketWhenItIsAPlainClaude() {
        assertThat(hostedSession().ticket()).isEmpty();
    }

    @Test
    void shouldRunTheVerbItWasLaunchedWithOnItsTicket() {
        assertThat(hostedSessionRunning(Verb.DESIGN).runs(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldNotRunAnotherVerbOnItsTicket() {
        assertThat(hostedSessionRunning(Verb.REVIEW).runs(linkedTicketId(), Verb.DESIGN)).isFalse();
    }

    @Test
    void shouldNotRunTheVerbOnAnotherTicket() {
        assertThat(hostedSessionRunning(Verb.DESIGN).runs(secondLinkedTicketId(), Verb.DESIGN)).isFalse();
    }

    @Test
    void shouldRunNoVerbWhenItIsAPlainClaude() {
        assertThat(hostedSession().runs(linkedTicketId(), Verb.DESIGN)).isFalse();
    }

    @Test
    void shouldBeNamedForTheTicketAndVerbItWasStartedFor() {
        assertThat(hostedSessionRunning(Verb.START).name()).isEqualTo(new SessionName("1 start"));
    }

    @Test
    void shouldBeNamedClaudeWhenItWasNotStartedForATicket() {
        assertThat(hostedSession().name()).isEqualTo(new SessionName("claude"));
    }

    @Test
    void shouldCloseOutTheTicketItWasStartedToDo() {
        assertThat(hostedSessionRunning(Verb.DONE).doneTicket()).contains(linkedTicketId());
    }

    @Test
    void shouldCloseOutNoTicketWhenItRanAnotherVerb() {
        assertThat(hostedSessionRunning(Verb.RESPOND).doneTicket()).isEmpty();
    }

    @Test
    void shouldCloseOutNoTicketWhenItWasNotStartedForOne() {
        assertThat(hostedSession().doneTicket()).isEmpty();
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.unknownSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.secondLinkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class HostedSessionTest {

    @Test
    void shouldNotBeBusyOrWaitingWhileStarting() {
        assertThat(new StartingPhase().isBusyOrWaiting()).isFalse();
    }

    @Test
    void shouldCountAnUnreadSessionAsBusySoQuittingAsks() {
        assertThat(new UnreadPhase(unknownSession()).isBusyOrWaiting())
                .as("with no record to read, a busy claude cannot be told from an idle one, and ending it cannot be undone")
                .isTrue();
    }

    @Test
    void shouldBeBusyOrWaitingWhenBusy() {
        assertThat(new LivePhase(sessionWith(new Busy())).isBusyOrWaiting()).isTrue();
    }

    @Test
    void shouldBeBusyOrWaitingWhenWaitingForPermission() {
        assertThat(new LivePhase(sessionWith(waitingForPermission())).isBusyOrWaiting()).isTrue();
    }

    @Test
    void shouldBeBusyOrWaitingWhenWaitingForInput() {
        assertThat(new LivePhase(sessionWith(new WaitingForInput(inputNeeded()))).isBusyOrWaiting()).isTrue();
    }

    @Test
    void shouldNotBeBusyOrWaitingOnYourTurn() {
        assertThat(new LivePhase(sessionWith(new YourTurn())).isBusyOrWaiting())
                .as("a session that waits for your next prompt loses nothing when lazyclaude quits")
                .isFalse();
    }

    @Test
    void shouldNotBeBusyOrWaitingWhenItsStateIsUnknown() {
        assertThat(new LivePhase(sessionWith(new Unknown())).isBusyOrWaiting()).isFalse();
    }

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

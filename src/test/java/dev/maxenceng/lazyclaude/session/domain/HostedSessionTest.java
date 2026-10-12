package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class HostedSessionTest {

    @Test
    void shouldBeStartingWhileNoRegistryRecordCarriesItsPid() {
        List<LiveSession> others = List.of(liveSessionBuilder().pid(secondProcessId()).build());

        assertThat(hostedSession().phaseAmong(others)).isEqualTo(new StartingPhase());
    }

    @Test
    void shouldBeStartingWhenNoSessionIsLive() {
        assertThat(hostedSession().phaseAmong(List.of())).isEqualTo(new StartingPhase());
    }

    @Test
    void shouldBeRegisteredAsTheLiveSessionWhosePidIsItsOwn() {
        LiveSession ours = liveSession();
        List<LiveSession> live = List.of(liveSessionBuilder().pid(secondProcessId()).build(), ours);

        assertThat(hostedSession().phaseAmong(live)).isEqualTo(new LivePhase(ours));
    }

    @Test
    void shouldNotBeBusyOrWaitingWhileStarting() {
        assertThat(new StartingPhase().isBusyOrWaiting()).isFalse();
    }

    @Test
    void shouldBeBusyOrWaitingWhenBusy() {
        assertThat(new LivePhase(sessionWith(new Busy())).isBusyOrWaiting()).isTrue();
    }

    @Test
    void shouldBeBusyOrWaitingWhenWaitingForPermission() {
        assertThat(new LivePhase(sessionWith(new WaitingForPermission())).isBusyOrWaiting()).isTrue();
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
    void shouldResumeTheSessionItWasStartedToResume() {
        assertThat(hostedSessionBuilder().command(new ResumeClaude(sessionId())).build().resumes(sessionId())).isTrue();
    }

    @Test
    void shouldNotResumeAnotherSession() {
        assertThat(hostedSessionBuilder().command(new ResumeClaude(sessionId())).build().resumes(secondSessionId())).isFalse();
    }

    @Test
    void shouldResumeNothingWhenStartedAsANewClaude() {
        assertThat(hostedSession().resumes(sessionId())).isFalse();
    }
}

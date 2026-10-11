package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
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

        assertThat(hostedSession().phaseAmong(live)).isEqualTo(new RegisteredPhase(ours));
    }

    @Test
    void shouldNotBeBusyOrWaitingWhileStarting() {
        assertThat(new StartingPhase().isBusyOrWaiting()).isFalse();
    }

    @Test
    void shouldBeBusyOrWaitingWhenBusy() {
        assertThat(new RegisteredPhase(sessionWith(new Busy())).isBusyOrWaiting()).isTrue();
    }

    @Test
    void shouldBeBusyOrWaitingWhenWaitingForPermission() {
        assertThat(new RegisteredPhase(sessionWith(new WaitingForPermission())).isBusyOrWaiting()).isTrue();
    }

    @Test
    void shouldBeBusyOrWaitingWhenWaitingForInput() {
        assertThat(new RegisteredPhase(sessionWith(new WaitingForInput(inputNeeded()))).isBusyOrWaiting()).isTrue();
    }

    @Test
    void shouldNotBeBusyOrWaitingOnYourTurn() {
        assertThat(new RegisteredPhase(sessionWith(new YourTurn())).isBusyOrWaiting())
                .as("a session that waits for your next prompt loses nothing when lazyclaude quits")
                .isFalse();
    }

    @Test
    void shouldNotBeBusyOrWaitingWhenItsStateIsUnknown() {
        assertThat(new RegisteredPhase(sessionWith(new Unknown())).isBusyOrWaiting()).isFalse();
    }
}

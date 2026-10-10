package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SessionStateTest {

    @Test
    void shouldBeBusyWhenTheStatusIsBusy() {
        assertThat(SessionState.of(ReportedStatus.BUSY, new NoWaitingReason())).isEqualTo(new Busy());
    }

    @Test
    void shouldBeBusyWhenTheStatusIsShell() {
        assertThat(SessionState.of(ReportedStatus.SHELL, new NoWaitingReason())).isEqualTo(new Busy());
    }

    @Test
    void shouldBeYourTurnWhenTheStatusIsIdle() {
        assertThat(SessionState.of(ReportedStatus.IDLE, new NoWaitingReason())).isEqualTo(new YourTurn());
    }

    @Test
    void shouldWaitForPermissionOnAPermissionPrompt() {
        assertThat(SessionState.of(ReportedStatus.WAITING, permissionPrompt())).isEqualTo(new WaitingForPermission());
    }

    @Test
    void shouldWaitForInputOnAnyOtherReason() {
        assertThat(SessionState.of(ReportedStatus.WAITING, inputNeeded())).isEqualTo(new WaitingForInput(inputNeeded()));
    }

    @Test
    void shouldWaitForInputWhenNoReasonIsGiven() {
        assertThat(SessionState.of(ReportedStatus.WAITING, new NoWaitingReason()))
                .isEqualTo(new WaitingForInput(new NoWaitingReason()));
    }

    @Test
    void shouldIgnoreAReasonOnASessionThatIsNotWaiting() {
        assertThat(SessionState.of(ReportedStatus.IDLE, permissionPrompt()))
                .as("only a waiting session is blocked, whatever stale reason its record carries")
                .isEqualTo(new YourTurn());
    }
}

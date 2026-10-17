package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.inputNotification;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.notificationId;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.secondNotificationId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.missingCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.argumentsGiven;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.commandFailing;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.commandHanging;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.commandPrinting;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.commandPrintingNothing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationUnavailableException;

class NotifySendRepositoryTest {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration IMPATIENCE = Duration.ofMillis(300);

    @TempDir
    Path root;

    @Test
    void shouldShowThroughNotifySendPrintingTheIdAndAnswerIt() throws IOException {
        Path notifySend = commandPrinting(root, "notify-send", "42");
        Path gdbus = commandPrinting(root, "gdbus", "()");

        assertThat(new NotifySendRepository(List.of(notifySend.toString()), List.of(gdbus.toString()), PATIENCE)
                        .show(inputNotification()))
                .isEqualTo(notificationId());
        assertThat(argumentsGiven(root, "notify-send")).containsExactly("-p -- app · lazyclaude-b6 input needed");
    }

    @Test
    void shouldReplaceInPlaceByTheIdItWasGiven() throws IOException {
        Path notifySend = commandPrinting(root, "notify-send", "42");
        Path gdbus = commandPrinting(root, "gdbus", "()");

        new NotifySendRepository(List.of(notifySend.toString()), List.of(gdbus.toString()), PATIENCE)
                .replace(notificationId(), inputNotification());

        assertThat(argumentsGiven(root, "notify-send")).containsExactly("-p -r 42 -- app · lazyclaude-b6 input needed");
    }

    @Test
    void shouldAnswerTheNewIdWhenTheReplacedNotificationWasAlreadyDismissed() throws IOException {
        Path notifySend = commandPrinting(root, "notify-send", "43");
        Path gdbus = commandPrinting(root, "gdbus", "()");

        assertThat(new NotifySendRepository(List.of(notifySend.toString()), List.of(gdbus.toString()), PATIENCE)
                        .replace(notificationId(), inputNotification()))
                .as("the desktop gives a new id when the one replaced is gone, and only that one can be closed later")
                .isEqualTo(secondNotificationId());
    }

    @Test
    void shouldCloseOverDBusBecauseNotifySendCannot() throws IOException {
        Path notifySend = commandPrinting(root, "notify-send", "42");
        Path gdbus = commandPrinting(root, "gdbus", "()");

        new NotifySendRepository(List.of(notifySend.toString()), List.of(gdbus.toString()), PATIENCE).close(notificationId());

        assertThat(argumentsGiven(root, "gdbus"))
                .containsExactly(
                        "call --session --dest org.freedesktop.Notifications --object-path /org/freedesktop/Notifications"
                                + " --method org.freedesktop.Notifications.CloseNotification 42");
    }

    @Test
    void shouldTreatANotificationTheUserAlreadyDismissedAsClosed() throws IOException {
        Path notifySend = commandPrinting(root, "notify-send", "42");
        Path gdbus = commandFailing(root, "gdbus", 1, "Error: GDBus.Error:org.freedesktop.Notifications.Error.NotFound");

        assertThatCode(() -> new NotifySendRepository(List.of(notifySend.toString()), List.of(gdbus.toString()), PATIENCE)
                        .close(notificationId()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldBeUnavailableWhenNotifySendIsNotOnThePath() {
        assertThatThrownBy(() -> new NotifySendRepository(List.of(missingCommand()), List.of(missingCommand()), PATIENCE)
                        .show(inputNotification()))
                .isInstanceOf(DesktopNotificationUnavailableException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldBeUnavailableWhenNotifySendExitsNonZero() throws IOException {
        Path notifySend = commandFailing(root, "notify-send", 2, "no bus");

        assertThatThrownBy(() -> new NotifySendRepository(List.of(notifySend.toString()), List.of(), PATIENCE)
                        .show(inputNotification()))
                .isInstanceOf(DesktopNotificationUnavailableException.class)
                .hasMessageContaining("exited with 2")
                .hasMessageContaining("no bus");
    }

    @Test
    void shouldBeUnavailableWhenNotifySendPrintsNoId() throws IOException {
        Path notifySend = commandPrintingNothing(root, "notify-send");

        assertThatThrownBy(() -> new NotifySendRepository(List.of(notifySend.toString()), List.of(), PATIENCE)
                        .show(inputNotification()))
                .isInstanceOf(DesktopNotificationUnavailableException.class)
                .hasMessageContaining("no notification id");
    }

    @Test
    void shouldBeUnavailableWhenGdbusFailsForAnotherReason() throws IOException {
        Path gdbus = commandFailing(root, "gdbus", 1, "Error: no session bus");

        assertThatThrownBy(() -> new NotifySendRepository(List.of(), List.of(gdbus.toString()), PATIENCE).close(notificationId()))
                .isInstanceOf(DesktopNotificationUnavailableException.class)
                .hasMessageContaining("no session bus");
    }

    @Test
    void shouldGiveUpOnACommandThatHangs() throws IOException {
        Path notifySend = commandHanging(root, "notify-send");

        assertThatThrownBy(() -> new NotifySendRepository(List.of(notifySend.toString()), List.of(), IMPATIENCE)
                        .show(inputNotification()))
                .isInstanceOf(DesktopNotificationUnavailableException.class)
                .hasMessageContaining("did not answer");
    }
}

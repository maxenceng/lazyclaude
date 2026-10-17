package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inKitty;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inTmux;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.missingCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.argumentsGiven;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.commandFailing;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.commandHanging;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.commandPrintingNothing;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeCommandFixture.jumpRepositoryOver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.RemoteControlOffException;
import dev.maxenceng.lazyclaude.session.domain.TerminalJumpFailedException;

class TerminalJumpRepositoryTest {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration IMPATIENCE = Duration.ofMillis(300);

    @TempDir
    Path root;

    @Test
    void shouldFocusTheKittyWindowOverTheSocketOfTheKittyThatOwnsIt() throws IOException {
        Path kitten = commandPrintingNothing(root, "kitten");

        jumpRepositoryOver(kitten, root.resolve("tmux"), PATIENCE).jump(inKitty());

        assertThat(argumentsGiven(root, "kitten")).containsExactly("@ --to unix:@kitty-1621 focus-window --match id:3");
    }

    @Test
    void shouldTellKittyRefusedBecauseNothingListensOnItsSocket() throws IOException {
        Path kitten = commandFailing(
                root, "kitten", 1, "Error: Failed to connect to unix:@kitty-1621 with error: dial unix @kitty-1621: connect: connection refused");

        assertThatThrownBy(() -> jumpRepositoryOver(kitten, root.resolve("tmux"), PATIENCE).jump(inKitty()))
                .isInstanceOf(RemoteControlOffException.class)
                .hasMessageContaining("connection refused");
    }

    @Test
    void shouldTellKittyRefusedBecauseRemoteControlIsDisabled() throws IOException {
        Path kitten = commandFailing(root, "kitten", 1, "Remote control is disabled");

        assertThatThrownBy(() -> jumpRepositoryOver(kitten, root.resolve("tmux"), PATIENCE).jump(inKitty()))
                .isInstanceOf(RemoteControlOffException.class);
    }

    @Test
    void shouldTellKittyRefusedBecauseRemoteControlIsAllowedOverASocketOnly() throws IOException {
        Path kitten = commandFailing(root, "kitten", 1, "Remote control is allowed over a socket only");

        assertThatThrownBy(() -> jumpRepositoryOver(kitten, root.resolve("tmux"), PATIENCE).jump(inKitty()))
                .isInstanceOf(RemoteControlOffException.class);
    }

    @Test
    void shouldNotTakeAnyOtherKittyFailureForRemoteControlBeingOff() throws IOException {
        Path kitten = commandFailing(root, "kitten", 1, "No matching windows for id:3");

        assertThatThrownBy(() -> jumpRepositoryOver(kitten, root.resolve("tmux"), PATIENCE).jump(inKitty()))
                .isInstanceOf(TerminalJumpFailedException.class)
                .hasMessageContaining("exited with 1")
                .hasMessageContaining("No matching windows");
    }

    @Test
    void shouldFailWhenKittenIsNotOnThePath() {
        assertThatThrownBy(() -> new TerminalJumpRepository(List.of(missingCommand()), List.of(), PATIENCE).jump(inKitty()))
                .isInstanceOf(TerminalJumpFailedException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldGiveUpOnAKittenThatHangs() throws IOException {
        Path kitten = commandHanging(root, "kitten");

        assertThatThrownBy(() -> jumpRepositoryOver(kitten, root.resolve("tmux"), IMPATIENCE).jump(inKitty()))
                .isInstanceOf(TerminalJumpFailedException.class)
                .hasMessageContaining("did not answer");
    }

    @Test
    void shouldSwitchTheTmuxClientToThePane() throws IOException {
        Path tmux = commandPrintingNothing(root, "tmux");

        assertThatCode(() -> jumpRepositoryOver(root.resolve("kitten"), tmux, PATIENCE).jump(inTmux())).doesNotThrowAnyException();

        assertThat(argumentsGiven(root, "tmux")).containsExactly("switch-client -t %7");
    }

    @Test
    void shouldFailWithWhatTmuxSaysWhenItRefuses() throws IOException {
        Path tmux = commandFailing(root, "tmux", 1, "no such pane: %7");

        assertThatThrownBy(() -> jumpRepositoryOver(root.resolve("kitten"), tmux, PATIENCE).jump(inTmux()))
                .isInstanceOf(TerminalJumpFailedException.class)
                .hasMessageContaining("exited with 1")
                .hasMessageContaining("no such pane");
    }

    @Test
    void shouldNotTakeATmuxFailureForRemoteControlBeingOff() throws IOException {
        Path tmux = commandFailing(root, "tmux", 1, "Remote control is disabled");

        assertThatThrownBy(() -> jumpRepositoryOver(root.resolve("kitten"), tmux, PATIENCE).jump(inTmux()))
                .isInstanceOf(TerminalJumpFailedException.class);
    }
}

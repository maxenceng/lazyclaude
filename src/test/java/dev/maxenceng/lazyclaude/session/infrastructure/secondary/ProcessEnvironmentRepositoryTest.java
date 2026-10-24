package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.emptyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.environmentOf;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyEnvironment;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.EnvironFixture.environOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.EnvironFixture.environThatIsADirectory;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.EnvironFixture.environWithAByteThatIsNotUtf8;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.EnvironmentUnreadableException;
import dev.maxenceng.lazyclaude.session.domain.EnvironmentVariableName;

class ProcessEnvironmentRepositoryTest {

    @TempDir
    Path proc;

    @Test
    void shouldReadTheVariablesAProcessWasStartedWith() throws IOException {
        environOf(proc, processId().value(), "KITTY_PID=1621", "KITTY_WINDOW_ID=3", "HOME=/home/max");

        assertThat(new ProcessEnvironmentRepository(proc, Map.of()).of(processId()))
                .isEqualTo(environmentOf(Map.of("KITTY_PID", "1621", "KITTY_WINDOW_ID", "3", "HOME", "/home/max")));
    }

    @Test
    void shouldKeepEverythingAfterTheFirstEqualsSignAsTheValue() throws IOException {
        environOf(proc, processId().value(), "TMUX=/tmp/tmux-1000/default,4242,0", "LESS=-R --prompt=a=b");

        assertThat(new ProcessEnvironmentRepository(proc, Map.of()).of(processId()))
                .isEqualTo(environmentOf(Map.of("TMUX", "/tmp/tmux-1000/default,4242,0", "LESS", "-R --prompt=a=b")));
    }

    @Test
    void shouldSkipAVariableSetToNothingAndAnEntryWithNoEqualsSign() throws IOException {
        environOf(proc, processId().value(), "EMPTY=", "STRAY", "KITTY_PID=1621");

        assertThat(new ProcessEnvironmentRepository(proc, Map.of()).of(processId()))
                .as("a variable set to nothing is not one, and a malformed entry has no name to keep")
                .isEqualTo(environmentOf(Map.of("KITTY_PID", "1621")));
    }

    @Test
    void shouldReadTheOtherVariablesWhenOneHoldsBytesThatAreNotUtf8() throws IOException {
        environWithAByteThatIsNotUtf8(proc, processId().value(), "KITTY_PID=1621", "KITTY_WINDOW_ID=3");

        assertThat(new ProcessEnvironmentRepository(proc, Map.of()).of(processId()).variables())
                .as("environ is whatever bytes the process was given, and one odd variable must not hide where it runs")
                .containsKeys(new EnvironmentVariableName("KITTY_PID"), new EnvironmentVariableName("KITTY_WINDOW_ID"));
    }

    @Test
    void shouldReadAnEmptyEnvironmentFromAProcessThatHasNone() throws IOException {
        environOf(proc, processId().value());

        assertThat(new ProcessEnvironmentRepository(proc, Map.of()).of(processId())).isEqualTo(emptyEnvironment());
    }

    @Test
    void shouldReadAnEmptyEnvironmentFromAProcessThatIsGone() {
        assertThat(new ProcessEnvironmentRepository(proc, Map.of()).of(processId()))
                .as("the process died between the registry read and this one")
                .isEqualTo(emptyEnvironment());
    }

    @Test
    void shouldSayWhyAnEnvironmentThatIsThereCannotBeRead() throws IOException {
        environThatIsADirectory(proc, processId().value());

        assertThatThrownBy(() -> new ProcessEnvironmentRepository(proc, Map.of()).of(processId()))
                .isInstanceOf(EnvironmentUnreadableException.class)
                .hasMessageContaining("environ");
    }

    @Test
    void shouldHandOverTheEnvironmentLazyclaudeWasStartedWith() {
        assertThat(new ProcessEnvironmentRepository(proc, Map.of("KITTY_PID", "1621", "KITTY_WINDOW_ID", "3")).ownEnvironment())
                .isEqualTo(kittyEnvironment());
    }
}

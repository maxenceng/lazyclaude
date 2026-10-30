package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.ptyBytesOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.awaited;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.exitOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.hostOfCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.hostRunning;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.newLaunch;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.newLaunchIn;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.resumeLaunch;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.ticketLaunch;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSizeOf;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

class PtyHostRepositoryTest {

    private final SessionId id = sessionId();
    private final PtyOutput output = new PtyOutput();

    @Test
    void shouldReturnThePidOfTheChild() {
        PtyHostRepository host = hostRunning("read line");

        assertThat(host.spawn(newLaunch(id), output, code -> {}).value()).isPositive();
        host.end(id);
    }

    @Test
    void shouldHandOverWhatTheChildPrintsAsBytes() throws Exception {
        PtyHostRepository host = hostRunning("printf 'hello pane'; read line");
        host.spawn(newLaunch(id), output, code -> {});

        assertThat(output.textContaining("hello pane")).startsWith("hello pane");
        host.end(id);
    }

    @Test
    void shouldHandOverTheBytesOfAnEscapeSequenceUnread() throws Exception {
        PtyHostRepository host = hostRunning("printf '\\033[1;31mred\\033[0m'; read line");
        host.spawn(newLaunch(id), output, code -> {});

        assertThat(output.textContaining("red")).as("an emulator is the frontend's, so the host must not interpret escapes").contains("\u001b[1;31mred\u001b[0m");
        host.end(id);
    }

    @Test
    void shouldHandOverTheOutputOnTheHostsReaderThread() throws Exception {
        PtyHostRepository host = hostRunning("printf 'drawn'; read line");
        host.spawn(newLaunch(id), output, code -> {});
        output.textContaining("drawn");

        assertThat(output.readerThreads()).allMatch(thread -> thread.startsWith("pty-"));
        host.end(id);
    }

    @Test
    void shouldReportACleanExitAsZero() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();

        hostRunning("exit 0").spawn(newLaunch(id), output, exit::complete);

        assertThat(awaited(exit)).isEqualTo(new ExitCode(0));
    }

    @Test
    void shouldReportANonZeroExitCode() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();

        hostRunning("exit 3").spawn(newLaunch(id), output, exit::complete);

        assertThat(awaited(exit)).isEqualTo(new ExitCode(3));
    }

    @Test
    void shouldHandOverEverythingTheChildPrintedBeforeReportingItsExit() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();

        hostRunning("printf 'bye'").spawn(newLaunch(id), output, exit::complete);
        awaited(exit);

        assertThat(output.text()).as("the exit is the last news of a session, after all its output").contains("bye");
    }

    @Test
    void shouldForgetASessionOnceItsChildExited() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();
        PtyHostRepository host = hostRunning("printf 'bye'");
        host.spawn(newLaunch(id), output, exit::complete);
        awaited(exit);

        host.write(id, ptyBytesOf("late"));
        host.resize(id, paneSizeOf(90, 20));
        host.end(id);

        assertThat(output.text()).as("a session that is gone ignores what is sent to it").doesNotContain("late");
    }

    @Test
    void shouldEndAndReportAChildThatClosedItsOutputButKeptRunning() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();

        hostRunning("exec </dev/null >/dev/null 2>&1; sleep 60").spawn(newLaunch(id), output, exit::complete);

        assertThat(awaited(exit).isFailure())
                .as("a claude nothing reads any more would leave a frozen tab that never closes")
                .isTrue();
    }

    @Test
    void shouldEndAndReportAChildWhoseOutputCouldNotBeHandled() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();
        PtyHostRepository host = hostRunning("printf 'drawn'; read line");

        host.spawn(newLaunch(id), drawn -> {
            throw new IllegalStateException("output listener failed");
        }, exit::complete);

        assertThat(awaited(exit).isFailure())
                .as("a reader that stopped on a failure still closes the tab and ends the claude behind it")
                .isTrue();
    }

    @Test
    void shouldEndTheChildOnRequest() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();
        PtyHostRepository host = hostRunning("exec sleep 60");
        host.spawn(newLaunch(id), output, exit::complete);

        host.end(id);

        assertThat(awaited(exit).isFailure()).as("a child killed by a signal did not exit cleanly").isTrue();
    }

    @Test
    void shouldRunTheChildInTheLaunchDirectory(@TempDir Path directory) throws Exception {
        PtyHostRepository host = hostRunning("pwd; read line");

        host.spawn(newLaunchIn(id, directory, 120, 30), output, code -> {});

        assertThat(output.textContaining(directory.getFileName().toString())).contains(directory.toString());
        host.end(id);
    }

    @Test
    void shouldAddResumeAndTheSessionIdToTheCommand() throws Exception {
        PtyHostRepository host = hostRunning("echo \"args: $*\"; read line");

        host.spawn(resumeLaunch(id), output, code -> {});

        assertThat(output.textContaining("args:")).contains("args: --resume " + id.value());
        host.end(id);
    }

    @Test
    void shouldNameTheSessionAfterItsTicketAndVerbAndPassTheTicketCommandAsOneArgument() throws Exception {
        PtyHostRepository host = hostRunning("printf 'args:'; printf '[%s]' \"$@\"; read line");

        host.spawn(ticketLaunch(id), output, code -> {});

        assertThat(output.textContaining("[/ticket review 1]"))
                .contains("args:[--session-id][" + id.value() + "][-n][1 review][/ticket review 1]");
        host.end(id);
    }

    @Test
    void shouldNameTheSessionOfACommandThatBeginsANewOne() throws Exception {
        PtyHostRepository host = hostRunning("echo \"args:[$*]\"; read line");

        host.spawn(newLaunch(id), output, code -> {});

        assertThat(output.textContaining("args:")).contains("args:[--session-id " + id.value() + "]");
        host.end(id);
    }

    @Test
    void shouldGiveTheChildNoNestedSessionKittyOrTmuxVariable() throws Exception {
        PtyHostRepository host = hostRunning(
                "env | sort; read line",
                Map.of(
                        "CLAUDECODE", "1",
                        "CLAUDE_CODE_ENTRYPOINT", "cli",
                        "KITTY_WINDOW_ID", "7",
                        "TMUX", "/tmp/tmux-1000/default,1,0",
                        "KEEP_ME", "kept"));

        host.spawn(newLaunchIn(id, Path.of("/tmp"), 100, 60), output, code -> {});
        String shown = output.textContaining("KEEP_ME=kept");

        assertThat(shown).doesNotContain("CLAUDE").doesNotContain("KITTY").doesNotContain("TMUX").contains("TERM=xterm-256color");
        host.end(id);
    }

    @Test
    void shouldWriteTheBytesToTheChild() throws Exception {
        PtyHostRepository host = hostRunning("exec /bin/cat");
        host.spawn(newLaunch(id), output, code -> {});

        host.write(id, ptyBytesOf("hi\r"));

        assertThat(output.textContaining("hi")).contains("hi");
        host.end(id);
    }

    @Test
    void shouldWriteTheBytesUnchangedSoCtrlCReachesTheChildAsAnInterrupt() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();
        PtyHostRepository host = hostRunning("trap 'exit 7' INT; echo ready; while true; do sleep 0.05; done");
        host.spawn(newLaunch(id), output, exit::complete);
        output.textContaining("ready");

        host.write(id, new PtyBytes(new byte[] {3}));

        assertThat(awaited(exit)).as("Ctrl+C reached the child as SIGINT, which lazyclaude must never take for itself").isEqualTo(new ExitCode(7));
    }

    @Test
    void shouldWriteEscapeSequencesUnchanged() throws Exception {
        PtyHostRepository host = hostRunning("exec /bin/cat -vT");
        host.spawn(newLaunch(id), output, code -> {});

        host.write(id, ptyBytesOf("\t\u001b[Z\r"));

        assertThat(output.textContaining("^I^[[Z")).contains("^I^[[Z");
        host.end(id);
    }

    @Test
    void shouldResizeThePtyOfTheChild() throws Exception {
        PtyHostRepository host = hostRunning("trap 'stty size' WINCH; stty size; while true; do sleep 0.05; done");
        host.spawn(newLaunchIn(id, Path.of("/tmp"), 100, 30), output, code -> {});
        output.textContaining("30 100");

        host.resize(id, paneSizeOf(120, 40));

        assertThat(output.textContaining("40 120")).contains("40 120");
        host.end(id);
    }

    @Test
    void shouldFailToStartWhenTheCommandDoesNotExist() {
        PtyHostRepository host = hostOfCommand("/nonexistent/claude");

        assertThatThrownBy(() -> host.spawn(newLaunch(id), output, code -> {}))
                .isInstanceOf(HostUnavailableException.class)
                .hasMessageStartingWith("claude could not be started:");
    }

    @Test
    void shouldIgnoreBytesForASessionItNeverStarted() {
        hostRunning("true").write(id, ptyBytesOf("a"));
    }

    @Test
    void shouldSayWhetherADirectoryExists(@TempDir Path directory) {
        PtyHostRepository host = hostRunning("true");

        assertThat(host.exists(new WorkingDirectory(directory))).isTrue();
        assertThat(host.exists(new WorkingDirectory(directory.resolve("gone")))).isFalse();
    }
}

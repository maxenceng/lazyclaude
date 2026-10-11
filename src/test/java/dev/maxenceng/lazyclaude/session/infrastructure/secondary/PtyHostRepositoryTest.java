package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.awaited;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.exitOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.hostOfCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.hostRunning;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.newLaunch;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.newLaunchIn;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.resumeLaunch;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.screenContaining;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.text;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSizeOf;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.leftPressAt;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.pasted;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.pressed;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.typed;
import static dev.maxenceng.lazyclaude.session.published.TerminalScreenFixture.styled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostUnavailableException;
import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.CursorAt;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.IndexedColour;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.NoCursor;
import dev.maxenceng.lazyclaude.session.published.RgbColour;
import dev.maxenceng.lazyclaude.session.published.StyledRun;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.session.published.TextAttribute;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

class PtyHostRepositoryTest {

    private final HostedSessionId id = HostedSessionId.random();

    @Test
    void shouldReturnThePidOfTheChild() {
        PtyHostRepository host = hostRunning("read line");

        assertThat(host.spawn(newLaunch(id), code -> {}).value()).isPositive();
        host.end(id);
    }

    @Test
    void shouldDrawWhatTheChildPrints() throws Exception {
        PtyHostRepository host = hostRunning("printf 'hello pane'; read line");
        host.spawn(newLaunch(id), code -> {});

        assertThat(text(screenContaining(host, id, "hello pane"))).startsWith("hello pane");
        host.end(id);
    }

    @Test
    void shouldTellThatTheScreenChanged() throws Exception {
        PtyHostRepository host = hostRunning("printf 'drawn'; read line");
        CompletableFuture<HostedSessionId> drew = new CompletableFuture<>();
        host.onScreenChange(drew::complete);

        host.spawn(newLaunch(id), code -> {});

        assertThat(drew.get(10, TimeUnit.SECONDS)).isEqualTo(id);
        host.end(id);
    }

    @Test
    void shouldReportACleanExitAsZero() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();

        hostRunning("exit 0").spawn(newLaunch(id), exit::complete);

        assertThat(awaited(exit)).isEqualTo(new ExitCode(0));
    }

    @Test
    void shouldReportANonZeroExitCode() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();

        hostRunning("exit 3").spawn(newLaunch(id), exit::complete);

        assertThat(awaited(exit)).isEqualTo(new ExitCode(3));
    }

    @Test
    void shouldForgetASessionOnceItsChildExited() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();
        PtyHostRepository host = hostRunning("printf 'bye'");

        host.spawn(newLaunch(id), exit::complete);
        awaited(exit);

        assertThat(text(host.screenOf(id)))
                .as("a session that is gone has nothing to read, and the host forgets it")
                .isEmpty();
    }

    @Test
    void shouldEndTheChildOnRequest() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();
        PtyHostRepository host = hostRunning("exec sleep 60");
        host.spawn(newLaunch(id), exit::complete);

        host.end(id);

        assertThat(awaited(exit).isFailure()).as("a child killed by a signal did not exit cleanly").isTrue();
    }

    @Test
    void shouldRunTheChildInTheLaunchDirectory(@TempDir Path directory) throws Exception {
        PtyHostRepository host = hostRunning("pwd; read line");

        host.spawn(newLaunchIn(id, directory, 120, 30), code -> {});

        assertThat(text(screenContaining(host, id, directory.getFileName().toString()))).contains(directory.toString());
        host.end(id);
    }

    @Test
    void shouldAddResumeAndTheSessionIdToTheCommand() throws Exception {
        PtyHostRepository host = hostRunning("echo \"args: $*\"; read line");

        host.spawn(resumeLaunch(id), code -> {});

        assertThat(text(screenContaining(host, id, "args:"))).contains("args: --resume " + sessionId().value());
        host.end(id);
    }

    @Test
    void shouldAddNothingToACommandThatBeginsANewSession() throws Exception {
        PtyHostRepository host = hostRunning("echo \"args:[$*]\"; read line");

        host.spawn(newLaunch(id), code -> {});

        assertThat(text(screenContaining(host, id, "args:"))).contains("args:[]");
        host.end(id);
    }

    @Test
    void shouldGiveTheChildNoClaudeKittyOrTmuxVariable() throws Exception {
        PtyHostRepository host = hostRunning(
                "env | sort; read line",
                Map.of(
                        "CLAUDECODE", "1",
                        "CLAUDE_CODE_ENTRYPOINT", "cli",
                        "KITTY_WINDOW_ID", "7",
                        "TMUX", "/tmp/tmux-1000/default,1,0",
                        "KEEP_ME", "kept"));

        host.spawn(newLaunchIn(id, Path.of("/tmp"), 100, 60), code -> {});
        String shown = text(screenContaining(host, id, "KEEP_ME=kept"));

        assertThat(shown).doesNotContain("CLAUDE").doesNotContain("KITTY").doesNotContain("TMUX").contains("TERM=xterm-256color");
        host.end(id);
    }

    @Test
    void shouldTypeIntoTheChild() throws Exception {
        PtyHostRepository host = hostOfCommand("/bin/cat");
        host.spawn(newLaunch(id), code -> {});

        host.send(id, typed('h', Modifiers.none()));
        host.send(id, typed('i', Modifiers.none()));

        assertThat(text(screenContaining(host, id, "hi"))).startsWith("hi");
        host.end(id);
    }

    @Test
    void shouldReachTheChildWithCtrlCAsAnInterrupt() throws Exception {
        CompletableFuture<ExitCode> exit = exitOf();
        PtyHostRepository host = hostRunning("trap 'exit 7' INT; echo ready; while true; do sleep 0.05; done");
        host.spawn(newLaunch(id), exit::complete);
        screenContaining(host, id, "ready");

        host.send(id, typed('c', Modifiers.of(Modifier.CTRL)));

        assertThat(awaited(exit)).as("Ctrl+C reached the child as SIGINT, which lazyclaude must never take for itself").isEqualTo(new ExitCode(7));
    }

    @Test
    void shouldReachTheChildWithTabAndShiftTab() throws Exception {
        PtyHostRepository host = hostOfCommand("/bin/cat", "-vT");
        host.spawn(newLaunch(id), code -> {});

        host.send(id, pressed(NamedKey.TAB));
        host.send(id, pressed(NamedKey.TAB, Modifiers.of(Modifier.SHIFT)));
        host.send(id, pressed(NamedKey.ENTER));

        assertThat(text(screenContaining(host, id, "^I^[[Z"))).contains("^I^[[Z");
        host.end(id);
    }

    @Test
    void shouldPasteInsideBracketsWhenTheChildTurnedBracketedPasteOn() throws Exception {
        PtyHostRepository host = hostRunning("printf '\\033[?2004h'; echo ready; cat -v");
        host.spawn(newLaunch(id), code -> {});
        screenContaining(host, id, "ready");

        host.send(id, pasted("x"));
        host.send(id, pressed(NamedKey.ENTER));

        assertThat(text(screenContaining(host, id, "^[[200~x^[[201~"))).contains("^[[200~x^[[201~");
        host.end(id);
    }

    @Test
    void shouldPasteBareWhenTheChildNeverTurnedBracketedPasteOn() throws Exception {
        PtyHostRepository host = hostOfCommand("/bin/cat", "-v");
        host.spawn(newLaunch(id), code -> {});

        host.send(id, pasted("plain"));

        assertThat(text(screenContaining(host, id, "plain")))
                .as("a child that did not ask for brackets must not see them")
                .doesNotContain("[200~");
        host.end(id);
    }

    @Test
    void shouldSendTheMouseToAChildThatAskedForIt() throws Exception {
        PtyHostRepository host = hostRunning("printf '\\033[?1000h\\033[?1006h'; echo ready; cat -v");
        host.spawn(newLaunch(id), code -> {});
        screenContaining(host, id, "ready");

        host.send(id, leftPressAt(4, 2));
        host.send(id, pressed(NamedKey.ENTER));

        assertThat(text(screenContaining(host, id, "^[[<0;5;3M"))).contains("^[[<0;5;3M");
        host.end(id);
    }

    @Test
    void shouldSendNoMouseToAChildThatDidNotAskForIt() throws Exception {
        PtyHostRepository host = hostOfCommand("/bin/cat", "-v");
        host.spawn(newLaunch(id), code -> {});

        host.send(id, leftPressAt(4, 2));
        host.send(id, typed('z', Modifiers.none()));
        host.send(id, pressed(NamedKey.ENTER));

        assertThat(text(screenContaining(host, id, "z")))
                .as("mouse bytes a child never enabled would be typed into its prompt")
                .doesNotContain("[<");
        host.end(id);
    }

    @Test
    void shouldResizeThePtyOfTheChild() throws Exception {
        PtyHostRepository host = hostRunning("trap 'stty size' WINCH; stty size; while true; do sleep 0.05; done");
        host.spawn(newLaunchIn(id, Path.of("/tmp"), 100, 30), code -> {});
        screenContaining(host, id, "30 100");

        host.resize(id, paneSizeOf(120, 40));

        assertThat(text(screenContaining(host, id, "40 120"))).contains("40 120");
        host.end(id);
    }

    @Test
    void shouldResizeTheScreenToTheNewPane() throws Exception {
        PtyHostRepository host = hostRunning("read line");
        host.spawn(newLaunchIn(id, Path.of("/tmp"), 100, 30), code -> {});
        assertThat(host.screenOf(id).rows()).hasSize(30);

        host.resize(id, paneSizeOf(90, 20));

        assertThat(host.screenOf(id).rows()).hasSize(20);
        host.end(id);
    }

    @Test
    void shouldCopyColoursAndAttributesIntoRuns() throws Exception {
        PtyHostRepository host = hostRunning("printf '\\033[1;31mred\\033[0m \\033[38;2;255;100;0morange\\033[0m'; read line");
        host.spawn(newLaunch(id), code -> {});

        TerminalScreen screen = screenContaining(host, id, "orange");

        assertThat(screen.rows().getFirst().runs())
                .extracting(StyledRun::style)
                .contains(styled(new IndexedColour(1), TextAttribute.BOLD), styled(new RgbColour(0xFF6400)));
        host.end(id);
    }

    @Test
    void shouldPlaceTheCursorAfterWhatWasPrinted() throws Exception {
        PtyHostRepository host = hostRunning("printf 'hello'; read line");
        host.spawn(newLaunch(id), code -> {});

        TerminalScreen screen = screenContaining(host, id, "hello");

        assertThat(screen.cursor()).isEqualTo(CursorAt.builder().column(new CellColumn(5)).row(new CellRow(0)).build());
        host.end(id);
    }

    @Test
    void shouldHideTheCursorWhenTheChildHidesIt() throws Exception {
        PtyHostRepository host = hostRunning("printf '\\033[?25lhidden'; read line");
        host.spawn(newLaunch(id), code -> {});

        assertThat(screenContaining(host, id, "hidden").cursor()).isEqualTo(new NoCursor());
        host.end(id);
    }

    @Test
    void shouldFailToStartWhenTheCommandDoesNotExist() {
        PtyHostRepository host = hostOfCommand("/nonexistent/claude");

        assertThatThrownBy(() -> host.spawn(newLaunch(id), code -> {}))
                .isInstanceOf(HostUnavailableException.class)
                .hasMessageStartingWith("claude could not be started:");
    }

    @Test
    void shouldAnswerBlankForASessionItNeverStarted() {
        assertThat(hostRunning("true").screenOf(id)).isEqualTo(TerminalScreen.blank());
    }

    @Test
    void shouldIgnoreInputForASessionItNeverStarted() {
        hostRunning("true").send(id, typed('a', Modifiers.none()));
    }

    @Test
    void shouldSayWhetherADirectoryExists(@TempDir Path directory) {
        PtyHostRepository host = hostRunning("true");

        assertThat(host.exists(new WorkingDirectory(directory))).isTrue();
        assertThat(host.exists(new WorkingDirectory(directory.resolve("gone")))).isFalse();
    }
}

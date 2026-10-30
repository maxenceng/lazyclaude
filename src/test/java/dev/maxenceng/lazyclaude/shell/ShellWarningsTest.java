package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.twoWarningsView;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningView;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningViewOf;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellWarning;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;

class ShellWarningsTest {

    private static final int WIDTH = 80;
    private static final int ROOM_FOR_TEXT = WIDTH - "⚠ ".length();

    @Test
    void shouldLeaveTheStatusLineBlankWhenNoWarningWasRaised() {
        assertThat(Screen.of(shellWarning()).statusLine()).isBlank();
    }

    @Test
    void shouldLeaveEveryOtherRowAsItWasWhenAWarningIsShown() {
        List<String> without = Screen.of(shellWarning()).lines();
        List<String> with = Screen.of(shellWarning(warningView())).lines();

        assertThat(with.subList(1, with.size())).isEqualTo(without.subList(1, without.size()));
    }

    @Test
    void shouldShowOneWarningWithoutACountFlushRight() {
        String line = Screen.of(shellWarning(warningView())).statusLine();

        assertThat(line).endsWith("⚠ " + warningView().text()).hasSize(WIDTH);
        assertThat(line.stripLeading()).doesNotContain("1 ·");
    }

    @Test
    void shouldShowTwoWarningsAsTheCountAndTheNewest() {
        assertThat(Screen.of(shellWarning(twoWarningsView())).statusLine())
                .endsWith("⚠ 2 · " + twoWarningsView().text());
    }

    @Test
    void shouldDrawTheWarningInYellow() {
        assertThat(Screen.of(shellWarning(warningView())).foregroundText(Color.YELLOW))
                .contains("⚠ " + warningView().text());
    }

    @Test
    void shouldShowTheWholeTextWhenItFillsTheRoomExactly() {
        String text = "x".repeat(ROOM_FOR_TEXT);

        assertThat(Screen.of(shellWarning(warningViewOf(text, 1))).statusLine())
                .isEqualTo("⚠ " + text);
    }

    @Test
    void shouldCutTheTextWithAnEllipsisWhenItIsOneCharacterOver() {
        String text = "x".repeat(ROOM_FOR_TEXT + 1);

        assertThat(Screen.of(shellWarning(warningViewOf(text, 1))).statusLine())
                .isEqualTo("⚠ " + "x".repeat(ROOM_FOR_TEXT - 1) + "…");
    }

    @Test
    void shouldCutTheCountedTextWithAnEllipsisToo() {
        String text = "x".repeat(ROOM_FOR_TEXT);

        assertThat(Screen.of(shellWarning(warningViewOf(text, 12))).statusLine())
                .isEqualTo("⚠ 12 · " + "x".repeat(ROOM_FOR_TEXT - "12 · ".length() - 1) + "…");
    }

    @Test
    void shouldNeverWrapALongWarningOntoTheRowBelow() {
        Screen screen = Screen.of(shellWarning(warningViewOf("y".repeat(300), 1)));

        assertThat(screen.lines().get(1)).startsWith("╭");
        assertThat(screen.lines()).as("the status line stays one row").hasSize(24);
    }

    @Test
    void shouldShowTheNewestWarningWhenAnotherIsRaisedLater() {
        Screen screen = Screen.of(shellWarning(warningView(), twoWarningsView()));

        assertThat(screen.statusLine()).endsWith("2 · " + twoWarningsView().text()).doesNotContain(warningView().text());
    }

    @Test
    void shouldKeepTheWarningThroughKeysAndRedraws() {
        Shell shell = shellWarning(warningView());

        Screen.of(shell);
        shell.press(key('l'));
        shell.press(key('h'));
        shell.press(alt('j'));

        assertThat(Screen.of(shell).statusLine()).endsWith(warningView().text());
    }

    @Test
    void shouldLeaveTheStatusLineBlankOnceTheLastWarningIsWithdrawn() {
        Shell shell = shellWarning(warningView());

        shell.warningsCleared();

        assertThat(Screen.of(shell).statusLine()).isBlank();
    }
}

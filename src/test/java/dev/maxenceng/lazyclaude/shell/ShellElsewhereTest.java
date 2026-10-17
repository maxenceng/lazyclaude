package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.elsewhereGroupViewNamed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.namedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewElsewhere;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnElsewhere;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class ShellElsewhereTest {

    @Test
    void shouldListEachPlaceUnderAHeaderWithItsCountAndEachSessionAsGlyphAndName() {
        Shell shell = shellOnElsewhere(sessionsViewElsewhere(
                elsewhereGroupViewNamed(
                        "next-suggestions",
                        namedSessionView(sessionIdOf(1), "refactor-ports", waitingForPermission()),
                        namedSessionView(sessionIdOf(2), "add-endpoint", new Busy())),
                elsewhereGroupViewNamed("/tmp/scratch", namedSessionView(sessionIdOf(3), "scratch-notes", new YourTurn()))));

        assertThat(Screen.of(shell).boardPanelLines())
                .containsSubsequence(
                        "▾ next-suggestions (2)",
                        "  ! refactor-ports",
                        "  ∼ add-endpoint",
                        "▾ /tmp/scratch (1)",
                        "  • scratch-notes");
    }

    @Test
    void shouldMoveBetweenSessionRowsAndSkipTheHeaders() {
        Shell shell = shellOnElsewhere(sessionsViewElsewhere(
                elsewhereGroupViewNamed("one", namedSessionView(sessionIdOf(1), "first", waitingForInput())),
                elsewhereGroupViewNamed("two", namedSessionView(sessionIdOf(2), "second", new Busy()))));

        shell.press(key('j'));

        assertThat(Screen.of(shell).reversedText()).containsExactly("∼ second");
        shell.press(key('k'));
        assertThat(Screen.of(shell).reversedText()).containsExactly("? first");
    }

    @Test
    void shouldShowTheSelectedSessionsFactsInTheMainView() {
        Shell shell = shellOnElsewhere(sessionsViewElsewhere(elsewhereGroupViewNamed(
                "next-suggestions", namedSessionView(sessionIdOf(1), "refactor-ports", waitingForPermission()))));

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence(
                        "refactor-ports  ! waiting for permission",
                        "project  next-suggestions",
                        "waiting  permission prompt",
                        "cwd      /work/app");
    }

    @Test
    void shouldDoNothingOnEnter() {
        Shell shell = shellOnElsewhere(sessionsViewElsewhere(
                elsewhereGroupViewNamed("one", namedSessionView(sessionIdOf(1), "first", waitingForInput()))));
        String before = Screen.of(shell).text();

        shell.press(enter());

        assertThat(Screen.of(shell).text()).isEqualTo(before);
        assertThat(shell.focus()).isEqualTo(Focus.ELSEWHERE);
    }

    @Test
    void shouldSayWhenNothingRunsElsewhere() {
        Shell shell = shellOnElsewhere(sessionsViewElsewhere());

        assertThat(Screen.of(shell).boardPanelText()).contains("nothing elsewhere");
    }
}

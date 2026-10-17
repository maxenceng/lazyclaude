package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.elsewhereGroupViewNamed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.namedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithElsewhere;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningViewOf;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnSessions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.style.Color;

class ShellElsewhereStatusTest {

    @Test
    void shouldNameEachOtherProjectWithAWaitingSessionUnderItsStrongestWaitingMarker() {
        Shell shell = shellOnSessions(sessionsViewWithElsewhere(
                List.of(namedSessionView(sessionIdOf(1), "mine", waitingForInput())),
                elsewhereGroupViewNamed(
                        "next-suggestions",
                        namedSessionView(sessionIdOf(2), "one", waitingForInput()),
                        namedSessionView(sessionIdOf(3), "two", waitingForPermission())),
                elsewhereGroupViewNamed("claude-tools", namedSessionView(sessionIdOf(4), "three", waitingForInput()))));

        Screen screen = Screen.of(shell);

        assertThat(screen.statusLine()).startsWith("? 1   elsewhere: ! next-suggestions  ? claude-tools");
        assertThat(screen.foregroundTextOnLine(0, Color.RED)).isEqualTo("!");
    }

    @Test
    void shouldNotListAProjectWhoseSessionsAreAtMostOnYourTurn() {
        Shell shell = shellOnSessions(sessionsViewWithElsewhere(
                List.of(),
                elsewhereGroupViewNamed(
                        "next-suggestions",
                        namedSessionView(sessionIdOf(2), "one", new YourTurn()),
                        namedSessionView(sessionIdOf(3), "two", new Busy()))));

        assertThat(Screen.of(shell).statusLine()).isBlank();
    }

    @Test
    void shouldStartWithElsewhereWhenTheProjectHasNoMarker() {
        Shell shell = shellOnSessions(sessionsViewWithElsewhere(
                List.of(), elsewhereGroupViewNamed("claude-tools", namedSessionView(sessionIdOf(4), "three", waitingForInput()))));

        assertThat(Screen.of(shell).statusLine()).startsWith("elsewhere: ? claude-tools");
    }

    @Test
    void shouldCutTheOverflowWithAnEllipsisBeforeTheWarning() {
        Shell shell = shellOnSessions(sessionsViewWithElsewhere(
                List.of(),
                elsewhereGroupViewNamed("a-very-long-project-name", namedSessionView(sessionIdOf(1), "one", waitingForInput())),
                elsewhereGroupViewNamed("another-very-long-name", namedSessionView(sessionIdOf(2), "two", waitingForInput())),
                elsewhereGroupViewNamed("yet-another-long-name", namedSessionView(sessionIdOf(3), "three", waitingForInput()))));
        shell.warningsChanged(warningViewOf("cannot watch", 1));

        assertThat(Screen.of(shell).statusLine())
                .matches("elsewhere: .*… ⚠ .*")
                .endsWith("⚠ cannot watch");
    }
}

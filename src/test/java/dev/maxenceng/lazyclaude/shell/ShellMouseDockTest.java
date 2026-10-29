package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.leftPress;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellInDock;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellWithTwoTabs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.layout.Position;

class ShellMouseDockTest {

    private final HostedSessionCommand hosting = mock(HostedSessionCommand.class, ShellFixture::noHostStarted);

    @Test
    void shouldSelectATabAndFocusTheDockWithAClickOnItsLabel() {
        Shell shell = shellWithTwoTabs(hosting);
        Position label = Screen.of(shell).dockTabLabel("second-one");

        assertThat(shell.mouse(leftPress(label))).isTrue();

        Screen drawn = Screen.of(shell);
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        assertThat(drawn.reversedTextOnLine(drawn.lineOf("╭dock"))).isEqualTo("second-one");
    }

    @Test
    void shouldSelectATabFromItsStateGlyphAsMuchAsFromItsName() {
        Shell shell = shellWithTwoTabs(hosting);
        Position name = Screen.of(shell).dockTabLabel("second-one");

        shell.mouse(leftPress(new Position(name.x() - 2, name.y())));

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        Screen drawn = Screen.of(shell);
        assertThat(drawn.reversedTextOnLine(drawn.lineOf("╭dock"))).isEqualTo("second-one");
    }

    @Test
    void shouldSelectATabFromTheLastCellOfItsLabel() {
        Shell shell = shellWithTwoTabs(hosting);
        Position name = Screen.of(shell).dockTabLabel("first-one");

        shell.mouse(leftPress(new Position(name.x() + "first-one".length() - 1, name.y())));

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldIgnoreAClickOnTheSeparatorBetweenTwoLabelsAndOnTheCellBeforeTheFirst() {
        Shell shell = shellWithTwoTabs(hosting);
        Position first = Screen.of(shell).dockTabLabel("first-one");
        Position second = Screen.of(shell).dockTabLabel("second-one");

        assertThat(shell.mouse(leftPress(new Position(second.x() - 3, second.y())))).isFalse();
        assertThat(shell.mouse(leftPress(new Position(first.x() + "first-one".length(), first.y())))).isFalse();
        assertThat(shell.mouse(leftPress(new Position(first.x() - 3, first.y())))).isFalse();

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldIgnoreAClickOnTheTitleRuleBesideTheLastLabel() {
        Shell shell = shellWithTwoTabs(hosting);
        Position second = Screen.of(shell).dockTabLabel("second-one");

        assertThat(shell.mouse(leftPress(new Position(second.x() + "second-one".length() + 3, second.y())))).isFalse();
    }

    @Test
    void shouldSelectATabFromTheStripAndEndTheZoom() {
        Shell shell = shellWithTwoTabs(hosting);
        shell.press(key('z'));
        Position label = Screen.of(shell).positionOf("second-one");

        assertThat(shell.mouse(leftPress(label))).isTrue();

        Screen drawn = Screen.of(shell);
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        assertThat(drawn.text()).contains("╭dock", "╭Board", "╭main view");
        assertThat(drawn.reversedTextOnLine(drawn.lineOf("╭dock"))).isEqualTo("second-one");
    }

    @Test
    void shouldIgnoreAClickOnTheStripBeyondTheLastTab() {
        Shell shell = shellWithTwoTabs(hosting);
        shell.press(key('z'));
        Position second = Screen.of(shell).positionOf("second-one");

        assertThat(shell.mouse(leftPress(new Position(second.x() + "second-one".length() + 6, second.y())))).isFalse();

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldKeepTheZoomOfTheDockWhenATabLabelIsClicked() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), hostedTab(2, "second-one", new YourTurn())),
                hosting,
                blankScreens());
        shell.press(alt('z'));
        Position label = Screen.of(shell).dockTabLabel("second-one");

        shell.mouse(leftPress(label));

        Screen drawn = Screen.of(shell);
        assertThat(drawn.text()).doesNotContain("╭Board");
        assertThat(drawn.reversedTextOnLine(drawn.lineOf("╭dock"))).isEqualTo("second-one");
    }

    @Test
    void shouldReachNoTabWhenTheDockHasNone() {
        Shell shell = shellHosting(SessionsViewFixture.emptySessionsView(), hosting, blankScreens());
        Position title = new Position(Screen.of(shell).dockTabLabel("dock").x() + 5, Screen.of(shell).lineOf("╭dock"));

        assertThat(shell.mouse(leftPress(title))).isFalse();
    }
}

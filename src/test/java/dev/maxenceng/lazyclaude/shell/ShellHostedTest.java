package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.startingTabNumbered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TerminalScreenFixture.screenSaying;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankTerminals;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellInDock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminals;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.style.Color;

@ExtendWith(MockitoExtension.class)
class ShellHostedTest {

    @Mock
    private HostedSessionCommand hosting;

    private final HostedTerminals terminals = blankTerminals();

    private final SessionQuery screens = blankScreens();

    @Test
    void shouldShowNoHostedSessionInAnEmptyDock() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, terminals, screens);

        assertThat(Screen.of(shell).text()).contains("no hosted session");
    }

    @Test
    void shouldLabelAStartingTabStarting() {
        Shell shell = shellHosting(sessionsViewWithTabs(startingTabNumbered(1)), hosting, terminals, screens);

        assertThat(Screen.of(shell).dockTitle()).contains("dock").contains("starting");
    }

    @Test
    void shouldLabelARegisteredTabWithItsMarkerInColourAndItsName() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "lazyclaude-3f", new YourTurn())), hosting, terminals, screens);

        Screen screen = Screen.of(shell);

        assertThat(screen.dockTitle()).contains("• lazyclaude-3f");
        assertThat(screen.foregroundTextOnLine(screen.lineOf("╭dock"), Color.CYAN)).contains("•");
    }

    @Test
    void shouldLabelEveryTabInStartOrderAndHighlightTheSelectedOne() {
        Shell shell = shellHosting(
                sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), hostedTab(2, "second-one", new YourTurn())),
                hosting, terminals,
                screens);

        Screen screen = Screen.of(shell);

        assertThat(screen.dockTitle()).containsSubsequence("∼ first-one", "• second-one");
        assertThat(screen.reversedTextOnLine(screen.lineOf("╭dock"))).isEqualTo("first-one");
    }

    @Test
    void shouldDrawTheSelectedTabsScreen() {
        when(terminals.screenOf(hostedSessionIdOf(1))).thenReturn(screenSaying("claude says hello"));
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, terminals, screens);

        assertThat(Screen.of(shell).text()).contains("claude says hello");
    }

    @Test
    void shouldAccentTheDockFrameWhileTheDockHasFocus() {
        Shell shell = shellInDock(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, terminals, screens);

        Screen screen = Screen.of(shell);

        assertThat(screen.foregroundTextOnLine(screen.lineOf("╭dock"), Color.CYAN)).contains("╭");
    }

    @Test
    void shouldGiveTheDockTheWholeRightColumnWhileTheMainViewIsHidden() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, terminals, screens);
        int dockLineWithMainView = Screen.of(shell).lineOf("╭dock");

        shell.press(alt('m'));
        Screen hidden = Screen.of(shell);

        assertThat(hidden.text()).doesNotContain("╭main view");
        assertThat(hidden.lineOf("╭dock")).isLessThan(dockLineWithMainView);
    }
}

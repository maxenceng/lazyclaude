package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.pressed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.typed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.oneTabInDock;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellInDock;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shiftTab;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.tui.event.KeyCode;

@ExtendWith(MockitoExtension.class)
class ShellDockKeysTest {

    @Mock
    private HostedSessionCommand hosting;

    private final HostedEmulatorCommand emulating = mock(HostedEmulatorCommand.class);

    private final SessionQuery screens = blankScreens();

    @Test
    void shouldHandEveryKeyToClaudeWhileATabHasFocus() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        assertThat(shell.press(key('q'))).isTrue();
        assertThat(shell.quitRequested()).as("q is a letter inside a session").isFalse();
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('q', Modifiers.none())));
    }

    @Test
    void shouldHandTabShiftTabEscAndCtrlRToClaude() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        shell.press(namedKey(KeyCode.TAB));
        shell.press(shiftTab());
        shell.press(namedKey(KeyCode.ESCAPE));
        shell.press(ctrl('r'));

        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), pressed(NamedKey.TAB)));
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), pressed(NamedKey.TAB, Modifiers.of(Modifier.SHIFT))));
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), pressed(NamedKey.ESCAPE)));
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('r', Modifiers.of(Modifier.CTRL))));
    }

    @Test
    void shouldHandCtrlCToClaudeAndNeverQuit() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        assertThat(shell.press(ctrl('c'))).as("handled, so TamboUI does not quit on it").isTrue();

        assertThat(shell.quitRequested()).isFalse();
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('c', Modifiers.of(Modifier.CTRL))));
    }

    @Test
    void shouldSwallowCtrlZSoClaudeIsNeverStopped() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        assertThat(shell.press(ctrl('z'))).isTrue();

        verify(emulating, never()).send(any());
    }

    @Test
    void shouldTakeTheReservedChordsBeforeClaudeSeesThem() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        shell.press(alt('z'));
        shell.press(alt('w'));
        shell.press(alt('j'));

        verify(emulating, never()).send(any());
    }

    @Test
    void shouldHandAnAltChordClaudeMayUseThatIsNotReserved() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        shell.press(alt('b'));

        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('b', Modifiers.of(Modifier.ALT))));
    }

    @Test
    void shouldNotHandAKeyThatNeitherNamesACharacterNorAKeyToClaude() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        assertThat(shell.press(namedKey(KeyCode.UNKNOWN))).isTrue();

        verify(emulating, never()).send(any());
    }

    @Test
    void shouldTreatTheDockAsAPanelWhenItHoldsNoTab() {
        Shell shell = shellInDock(sessionsViewWithTabs(), hosting, emulating, screens);

        shell.press(key('q'));

        assertThat(shell.quitRequested()).isTrue();
        verify(emulating, never()).send(any());
    }

    @Test
    void shouldStepToTheNextTabWithAltLAndBackWithAltH() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(
                        hostedTab(1, "first-one", new Busy()),
                        hostedTab(2, "second-one", new YourTurn()),
                        hostedTab(3, "third-one", new YourTurn())),
                hosting, emulating,
                screens);

        shell.press(alt('l'));
        assertThat(Screen.of(shell).reversedTextOnLine(Screen.of(shell).lineOf("╭dock"))).isEqualTo("second-one");
        shell.press(alt('h'));

        assertThat(Screen.of(shell).reversedTextOnLine(Screen.of(shell).lineOf("╭dock"))).isEqualTo("first-one");
    }

    @Test
    void shouldStayOnTheLastTabWhenAltLHasNoNextOne() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), hostedTab(2, "second-one", new YourTurn())),
                hosting, emulating,
                screens);
        shell.press(alt('l'));

        assertThat(shell.press(alt('l'))).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        assertThat(Screen.of(shell).reversedTextOnLine(Screen.of(shell).lineOf("╭dock"))).isEqualTo("second-one");
    }

    @Test
    void shouldGoToThePanelsWithAltHOnTheFirstTab() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), hostedTab(2, "second-one", new YourTurn())),
                hosting, emulating,
                screens);

        shell.press(alt('h'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldGoToTheMainViewWithAltKFromTheDock() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        shell.press(alt('k'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
    }

    @Test
    void shouldShowAHiddenMainViewAndGoToItWithAltK() {
        Shell shell = oneTabInDock(hosting, emulating, screens);
        shell.press(alt('m'));
        assertThat(Screen.of(shell).text()).doesNotContain("╭main view");

        shell.press(alt('k'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).text()).contains("╭main view");
    }

    @Test
    void shouldShowAHiddenMainViewWithAltKFromADockWithNoTab() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, emulating, screens);
        shell.press(key('m'));
        shell.press(alt('l'));
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);

        shell.press(alt('k'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).text()).contains("╭main view");
    }

    @Test
    void shouldGoToTheDockWithAltJFromTheMainView() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, emulating, screens);
        shell.press(alt('l'));
        assertThat(shell.focus()).isEqualTo(Focus.MAIN);

        shell.press(alt('j'));

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldHideTheMainViewWithMOutsideASessionAndShowItAgainWithM() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, emulating, screens);

        shell.press(key('m'));
        assertThat(Screen.of(shell).text()).doesNotContain("╭main view");
        shell.press(key('m'));

        assertThat(Screen.of(shell).text()).contains("╭main view");
    }

    @Test
    void shouldHideTheMainViewWithAltMFromInsideASession() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        shell.press(alt('m'));

        assertThat(Screen.of(shell).text()).doesNotContain("╭main view");
        verify(emulating, never()).send(any());
    }

    @Test
    void shouldKeepTheMainViewHiddenUntilToggledBack() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, emulating, screens);
        shell.press(alt('m'));

        shell.press(key('j'));
        shell.press(key('l'));
        shell.press(alt('j'));

        assertThat(Screen.of(shell).text()).doesNotContain("╭main view");
    }

    @Test
    void shouldSendMToClaudeInsteadOfHidingTheMainViewInsideASession() {
        Shell shell = oneTabInDock(hosting, emulating, screens);

        shell.press(key('m'));

        assertThat(Screen.of(shell).text()).contains("╭main view");
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('m', Modifiers.none())));
    }

    @Test
    void shouldMoveFocusToTheDockWhenHidingTheMainViewFromIt() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, emulating, screens);
        shell.press(alt('l'));

        shell.press(alt('m'));

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldReachTheDockWithLFromAPanelWhileTheMainViewIsHidden() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, emulating, screens);
        shell.press(alt('m'));

        shell.press(key('l'));

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }
}

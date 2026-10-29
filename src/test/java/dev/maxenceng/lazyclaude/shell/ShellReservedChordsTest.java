package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.typed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.chord;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlY;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.rebinding;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.keyBar;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.oneTabInDockWithChords;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnBoardWithChords;
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
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

@ExtendWith(MockitoExtension.class)
class ShellReservedChordsTest {

    @Mock
    private HostedSessionCommand hosting;

    private final HostedEmulatorCommand emulating = mock(HostedEmulatorCommand.class);

    @Test
    void shouldFocusTheMainViewOnTheReboundChordOutsideASessionAndNoLongerOnTheOldOne() {
        Shell shell = shellOnBoardWithChords(rebinding(ReservedAction.FOCUS_RIGHT, chord('l', Modifier.CTRL)));

        shell.press(alt('l'));
        assertThat(keyBar(shell)).as("the old chord moves nothing").doesNotContain("j/k scroll");

        shell.press(ctrl('l'));
        assertThat(keyBar(shell)).contains("j/k scroll");
    }

    @Test
    void shouldFocusTheBoardBackOnTheReboundFocusLeftChordOutsideASession() {
        Shell shell = shellOnBoardWithChords(rebinding(ReservedAction.FOCUS_LEFT, chord('b', Modifier.CTRL)));
        shell.press(alt('l'));

        shell.press(alt('h'));
        assertThat(keyBar(shell)).as("the old chord moves nothing").contains("j/k scroll");

        shell.press(ctrl('b'));
        assertThat(keyBar(shell)).doesNotContain("j/k scroll").contains("j/k move");
    }

    @Test
    void shouldHideTheMainViewOnTheReboundChordOutsideASession() {
        Shell shell = shellOnBoardWithChords(rebinding(ReservedAction.HIDE_MAIN_VIEW, chord('e', Modifier.CTRL)));

        shell.press(alt('m'));
        assertThat(Screen.of(shell).text()).as("the old chord hides nothing").contains("╭main view");

        shell.press(ctrl('e'));
        assertThat(Screen.of(shell).text()).doesNotContain("╭main view");
    }

    @Test
    void shouldZoomOnTheReboundChordOutsideASession() {
        Shell shell = shellOnBoardWithChords(rebinding(ReservedAction.ZOOM, chord('g', Modifier.CTRL)));

        shell.press(alt('z'));
        assertThat(Screen.of(shell).text()).as("the old chord zooms nothing").contains("Sessions");

        shell.press(ctrl('g'));
        assertThat(Screen.of(shell).text()).doesNotContain("Sessions");
    }

    @Test
    void shouldLeaveTheZoomOnTheReboundNextWaitingChordAndNotOnTheOldOne() {
        Shell shell = shellOnBoardWithChords(rebinding(ReservedAction.NEXT_WAITING, chord('w', Modifier.CTRL)));
        shell.press(key('z'));

        shell.press(alt('w'));
        assertThat(Screen.of(shell).text()).as("the old chord leaves nothing").doesNotContain("Sessions");

        shell.press(ctrl('w'));
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldTakeTheReboundChordInsideASessionAndHandTheOldOneToClaude() {
        Shell shell = oneTabInDockWithChords(rebinding(ReservedAction.ZOOM, chord('g', Modifier.CTRL)), hosting, emulating);

        shell.press(ctrl('g'));
        assertThat(Screen.of(shell).text()).as("the dock fills the screen").doesNotContain("Sessions");
        verify(emulating, never()).send(any());

        shell.press(alt('z'));
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('z', Modifiers.of(Modifier.ALT))));
    }

    @Test
    void shouldMoveFocusOnTheReboundChordInsideASessionWithoutHandingItToClaude() {
        Shell shell = oneTabInDockWithChords(rebinding(ReservedAction.FOCUS_UP, chord('k', Modifier.CTRL)), hosting, emulating);

        shell.press(ctrl('k'));

        assertThat(keyBar(shell)).contains("j/k scroll");
        verify(emulating, never()).send(any());
    }

    @Test
    void shouldLeaveTheZoomOnTheReboundNextWaitingChordInsideASessionAndHandTheOldOneToClaude() {
        Shell shell = oneTabInDockWithChords(rebinding(ReservedAction.NEXT_WAITING, chord('w', Modifier.CTRL)), hosting, emulating);
        shell.press(alt('z'));

        shell.press(ctrl('w'));
        assertThat(Screen.of(shell).text()).contains("Sessions");
        verify(emulating, never()).send(any());

        shell.press(alt('w'));
        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('w', Modifiers.of(Modifier.ALT))));
    }

    @Test
    void shouldHandAnUppercaseKeyWithTheReservedModifiersToClaude() {
        Shell shell = oneTabInDockWithChords(ReservedChords.defaults(), hosting, emulating);

        shell.press(alt('H'));

        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('H', Modifiers.of(Modifier.ALT))));
    }

    @Test
    void shouldShowTheBoundChordsInTheKeyBarOutsideASession() {
        Shell shell = shellOnBoardWithChords(rebinding(ReservedAction.FOCUS_LEFT, ctrlY()));

        assertThat(keyBar(shell)).contains("ctrl+y/alt+j/alt+k/alt+l focus").doesNotContain("alt+h/j/k/l");
    }

    @Test
    void shouldShowTheBoundChordsInTheKeyBarOfASession() {
        Shell shell = oneTabInDockWithChords(rebinding(ReservedAction.FOCUS_LEFT, ctrlY()), hosting, emulating);

        assertThat(keyBar(shell)).contains("ctrl+y/alt+l tab  ctrl+y/alt+j/alt+k/alt+l focus");
    }

    @Test
    void shouldShowTheBoundZoomChordInTheKeyBarOfTheDockZoom() {
        Shell shell = oneTabInDockWithChords(rebinding(ReservedAction.ZOOM, chord('g', Modifier.CTRL)), hosting, emulating);
        shell.press(ctrl('g'));

        assertThat(keyBar(shell)).contains("alt+h/l tab  alt+h/j/k/l focus  alt+m main view  ctrl+g back");
    }

    @Test
    void shouldKeepTheCompactKeyBarWhileTheDefaultsStand() {
        Shell shell = oneTabInDockWithChords(ReservedChords.defaults(), hosting, emulating);

        assertThat(keyBar(shell)).contains("alt+h/l tab  alt+h/j/k/l focus  alt+m main view  alt+z zoom");
    }
}

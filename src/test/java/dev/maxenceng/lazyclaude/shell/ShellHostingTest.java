package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.startingTabNumbered;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.leftPressAt;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.wheelUpAt;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningViewOf;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellInDock;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.startedPayload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.PastedText;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.layout.Position;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

@ExtendWith(MockitoExtension.class)
class ShellHostingTest {

    @Mock
    private HostedSessionCommand hosting;

    private final SessionQuery screens = blankScreens();

    @Test
    void shouldStartAPlainClaudeInTheProjectRootWithNFromAnyPanel() {
        when(hosting.startUnlinked(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);
        shell.press(alt('j'));

        shell.press(key('N'));

        assertThat(startedPayload(hosting).root()).isEqualTo(boardViewOf(ticketView()).root().value());
    }

    @Test
    void shouldStartWithNFromTheMainViewToo() {
        when(hosting.startUnlinked(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);
        shell.press(alt('l'));

        shell.press(key('N'));

        verify(hosting).startUnlinked(any());
    }

    @Test
    void shouldStartAtTheSizeOfThePaneItIsDrawnIn() {
        when(hosting.startUnlinked(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);
        Screen.of(shell);

        shell.press(key('N'));

        assertThat(startedPayload(hosting).size().width().cells()).isEqualTo(50);
        assertThat(startedPayload(hosting).size().height().cells()).isEqualTo(9);
    }

    @Test
    void shouldFocusTheNewTabAndReadStartingUntilItsRecordAppears() {
        when(hosting.startUnlinked(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);

        shell.press(key('N'));
        shell.sessionsChanged(sessionsViewWithTabs(startingTabNumbered(1)));

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        assertThat(Screen.of(shell).dockTitle()).contains("starting");
    }

    @Test
    void shouldFocusTheNewTabAmongTheOpenOnes() {
        when(hosting.startUnlinked(any())).thenReturn(new StartedHost(hostedSessionIdOf(2)));
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "first-one", new Busy())), hosting, screens);

        shell.press(key('N'));
        shell.sessionsChanged(sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), startingTabNumbered(2)));
        Screen screen = Screen.of(shell);

        assertThat(screen.reversedTextOnLine(screen.lineOf("╭dock"))).isEqualTo("starting");
    }

    @Test
    void shouldOpenNoTabAndKeepFocusWhenSpawningFails() {
        when(hosting.startUnlinked(any())).thenReturn(new NotStartedHost());
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);

        shell.press(key('N'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).contains("no hosted session");
    }

    @Test
    void shouldListAHostedSessionWithPaneWhereItsAgeWouldBe() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "lazyclaude-3f", new Busy())), hosting, screens);
        shell.press(alt('j'));

        assertThat(Screen.of(shell).boardPanelLines()).anySatisfy(line -> assertThat(line).matches("∼ lazyclaude-3f\\s+pane"));
    }

    @Test
    void shouldFocusTheTabOfAHostedSessionWithEnter() {
        Shell shell = shellHosting(
                sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), hostedTab(2, "second-one", new YourTurn())),
                hosting,
                screens);
        shell.press(alt('j'));
        shell.press(key('j'));

        shell.press(enter());
        Screen screen = Screen.of(shell);

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        assertThat(screen.reversedTextOnLine(screen.lineOf("╭dock"))).isEqualTo("second-one");
    }

    @Test
    void shouldResumeAnUnlinkedPastSessionInItsRecordedDirectoryWithEnter() {
        when(hosting.resume(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(sessionsViewOf(List.of(), List.of(pastSessionView())), hosting, screens);
        shell.press(alt('j'));

        shell.press(enter());

        ArgumentCaptor<ResumeHostedPayload> payload = ArgumentCaptor.forClass(ResumeHostedPayload.class);
        verify(hosting).resume(payload.capture());
        assertThat(payload.getValue().session()).isEqualTo(pastSessionView().id());
        assertThat(payload.getValue().recordedDirectory()).isEqualTo(pastSessionView().directory());
        assertThat(payload.getValue().root()).as("the launch log that lists the session is the project's").isEqualTo(project().root());
    }

    @Test
    void shouldResumeAnInterruptedPastSessionWithEnterLikeAnyOther() {
        when(hosting.resume(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(
                sessionsViewOf(List.of(), List.of(pastSessionViewBuilder().interrupted(true).build())), hosting, screens);
        shell.press(alt('j'));

        shell.press(enter());

        verify(hosting).resume(any());
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldFocusTheResumedTab() {
        when(hosting.resume(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(sessionsViewOf(List.of(), List.of(pastSessionView())), hosting, screens);
        shell.press(alt('j'));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldKeepFocusWhenResumingFails() {
        when(hosting.resume(any())).thenReturn(new NotStartedHost());
        Shell shell = shellHosting(sessionsViewOf(List.of(), List.of(pastSessionView())), hosting, screens);
        shell.press(alt('j'));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
    }

    @Test
    void shouldMoveFocusToTheNextTabWhenTheFocusedOneCloses() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(
                        hostedTab(1, "first-one", new Busy()),
                        hostedTab(2, "second-one", new Busy()),
                        hostedTab(3, "third-one", new Busy())),
                hosting,
                screens);
        shell.press(alt('l'));

        shell.sessionsChanged(sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), hostedTab(3, "third-one", new Busy())));
        Screen screen = Screen.of(shell);

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        assertThat(screen.reversedTextOnLine(screen.lineOf("╭dock"))).isEqualTo("third-one");
    }

    @Test
    void shouldMoveFocusToThePanelsWhenTheLastTabCloses() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "first-one", new Busy()), hostedTab(2, "second-one", new Busy())),
                hosting,
                screens);
        shell.press(alt('l'));

        shell.sessionsChanged(sessionsViewWithTabs(hostedTab(1, "first-one", new Busy())));
        Screen.of(shell);

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldMoveFocusToThePanelsWhenTheOnlyTabCloses() {
        Shell shell = shellInDock(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        shell.sessionsChanged(sessionsViewWithTabs());
        Screen.of(shell);

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldLeaveFocusAloneWhenATabClosesWhileThePanelsHaveIt() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        shell.press(alt('j'));

        shell.sessionsChanged(sessionsViewWithTabs());
        Screen.of(shell);

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
    }

    @Test
    void shouldQuitAtOnceWhenNoHostedSessionIsBusyOrWaiting() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new YourTurn()), startingTabNumbered(2)), hosting, screens);

        shell.press(key('q'));

        assertThat(shell.quitRequested()).isTrue();
    }

    @Test
    void shouldAskBeforeQuittingWhenAHostedSessionIsBusy() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        shell.press(key('q'));

        assertThat(shell.quitRequested()).isFalse();
        assertThat(Screen.of(shell).statusLine()).contains("1 hosted session is busy or waiting — quit? y/n");
    }

    @Test
    void shouldCountTheBusyOrWaitingSessionsInThePrompt() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy()), hostedTab(2, "two", new Busy())), hosting, screens);

        shell.press(key('q'));

        assertThat(Screen.of(shell).statusLine()).contains("2 hosted sessions are busy or waiting — quit? y/n");
    }

    @Test
    void shouldShowTheQuitPromptOverAWarningRaisedEarlier() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        shell.warningsChanged(warningViewOf("resume: " + "/gone".repeat(40) + " is gone; running in the project root instead.", 1));

        shell.press(key('q'));

        assertThat(Screen.of(shell).statusLine())
                .as("a warning drawn over the question would hide what the next key answers")
                .contains("1 hosted session is busy or waiting — quit? y/n");
    }

    @Test
    void shouldQuitOnYes() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        shell.press(key('q'));

        shell.press(key('y'));

        assertThat(shell.quitRequested()).isTrue();
    }

    @Test
    void shouldStayOnNoAndClearThePrompt() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        shell.press(key('q'));

        shell.press(key('n'));

        assertThat(shell.quitRequested()).isFalse();
        assertThat(Screen.of(shell).statusLine()).doesNotContain("quit?");
    }

    @Test
    void shouldAskBeforeQuittingWithCtrlCOutsideASessionToo() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        shell.press(ctrl('c'));

        assertThat(shell.quitRequested()).isFalse();
        assertThat(Screen.of(shell).statusLine()).contains("quit? y/n");
    }

    @Test
    void shouldQuitWithCtrlCOutsideASessionWhenNothingIsBusy() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);

        shell.press(ctrl('c'));

        assertThat(shell.quitRequested()).isTrue();
    }

    @Test
    void shouldHandAPasteToTheFocusedTab() {
        Shell shell = shellInDock(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        shell.paste(new PastedText("one\ntwo"));

        verify(hosting).send(new HostedInputPayload(hostedSessionIdOf(1), new PastedText("one\ntwo")));
    }

    @Test
    void shouldDropAPasteWhileThePanelsHaveFocus() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        shell.paste(new PastedText("one\ntwo"));

        verify(hosting, never()).send(any());
    }

    @Test
    void shouldHandTheWheelOverThePaneToClaudeAtTheMatchingCell() {
        Shell shell = shellInDock(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        Position origin = Screen.of(shell).dockContentOrigin();

        assertThat(shell.mouse(MouseEvent.scrollUp(origin.x() + 3, origin.y() + 2))).isTrue();

        verify(hosting).send(new HostedInputPayload(hostedSessionIdOf(1), wheelUpAt(3, 2)));
    }

    @Test
    void shouldHandAClickOverThePaneToClaudeEvenWhileThePanelsHaveFocus() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        Position origin = Screen.of(shell).dockContentOrigin();

        shell.mouse(MouseEvent.press(MouseButton.LEFT, origin.x() + 4, origin.y() + 1));

        verify(hosting).send(new HostedInputPayload(hostedSessionIdOf(1), leftPressAt(4, 1)));
    }

    @Test
    void shouldLeaveAMouseEventOutsideThePaneUnhandled() {
        Shell shell = shellInDock(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        Screen.of(shell);

        assertThat(shell.mouse(MouseEvent.press(MouseButton.LEFT, 1, 1))).isFalse();

        verify(hosting, never()).send(any());
    }

    @Test
    void shouldLeaveAMouseEventUnhandledWhenThereIsNoTab() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);
        Position origin = Screen.of(shell).dockContentOrigin();

        assertThat(shell.mouse(MouseEvent.press(MouseButton.LEFT, origin.x(), origin.y()))).isFalse();
    }

    @Test
    void shouldResizeATabItDidNotStartToThePaneItIsDrawnIn() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        Screen.of(shell);

        ArgumentCaptor<ResizePayload> resized = ArgumentCaptor.forClass(ResizePayload.class);
        verify(hosting).resize(resized.capture());
        assertThat(resized.getValue().session()).isEqualTo(hostedSessionIdOf(1));
        assertThat(resized.getValue().size().width().cells()).isEqualTo(50);
        assertThat(resized.getValue().size().height().cells()).isEqualTo(9);
    }

    @Test
    void shouldNotResizeAgainWhileThePaneKeepsItsSize() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        Screen.of(shell);
        Screen.of(shell);

        verify(hosting, times(1)).resize(any());
    }

    @Test
    void shouldResizeWhenThePaneGrowsBecauseTheMainViewIsHidden() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        Screen.of(shell);

        shell.press(alt('m'));
        Screen.of(shell);

        ArgumentCaptor<ResizePayload> resized = ArgumentCaptor.forClass(ResizePayload.class);
        verify(hosting, times(2)).resize(resized.capture());
        assertThat(resized.getAllValues().getLast().size().height().cells())
                .isGreaterThan(resized.getAllValues().getFirst().size().height().cells());
    }

    @Test
    void shouldNotResizeATabItStartedAtThePaneSize() {
        when(hosting.startUnlinked(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);
        Screen.of(shell);
        shell.press(key('N'));
        shell.sessionsChanged(sessionsViewWithTabs(startingTabNumbered(1)));

        Screen.of(shell);

        verify(hosting, never()).resize(any());
    }

    @Test
    void shouldBuildAConfigWithBracketedPasteAndMouseCaptureOn() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);

        assertThat(shell.config().bracketedPaste()).isTrue();
        assertThat(shell.config().mouseCapture()).isTrue();
    }

    @Test
    void shouldLeaveTabAndShiftTabUnboundFromFocusSoTheyReachTheSession() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);
        Bindings bindings = shell.config().bindings();

        assertThat(bindings.triggersFor("focusNext")).isEmpty();
        assertThat(bindings.triggersFor("focusPrevious")).isEmpty();
    }

    @Test
    void shouldLeaveQuitUnboundSoOnlyTheShellDecidesWhenToQuit() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);

        assertThat(shell.config().bindings().triggersFor("quit"))
                .as("TamboUI's own quit binding takes Q too, and would end busy hosted sessions without asking")
                .isEmpty();
    }

    @Test
    void shouldListOnlyTheReservedChordsOverAFocusedTab() {
        Shell shell = shellInDock(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);

        String keyBar = Screen.of(shell).lines().getLast();

        assertThat(keyBar).contains("alt+h/j/k/l focus", "alt+m").doesNotContain("q quit", "j/k move", "enter");
    }

    @Test
    void shouldListNAndMOverThePanels() {
        Shell shell = shellHosting(sessionsViewWithTabs(), hosting, screens);

        assertThat(Screen.of(shell).lines().getLast()).contains("N new", "R refresh");
    }
}

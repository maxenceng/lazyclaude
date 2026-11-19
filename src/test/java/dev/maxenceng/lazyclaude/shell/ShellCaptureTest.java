package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.projectRoot;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.published.NotStartedHostFixture.notStartedBecause;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.shell.ShellCaptureFixture.newTicketPayload;
import static dev.maxenceng.lazyclaude.shell.ShellCaptureFixture.shellCapturing;
import static dev.maxenceng.lazyclaude.shell.ShellCaptureFixture.shellOpenForADescription;
import static dev.maxenceng.lazyclaude.shell.ShellCaptureFixture.typed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.escape;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

class ShellCaptureTest {

    private static final int WIDE = 120;
    private static final int HEIGHT = 24;

    private final HostedSessionCommand hosting = mock(HostedSessionCommand.class);
    private final BoardQuery boards = mock(BoardQuery.class);

    @Test
    void shouldOpenAOneLinePopupAskingForADescriptionWithCOnTheBoard() {
        Shell shell = shellCapturing(hosting, boards, Runnable::run, ticketView());

        shell.press(key('c'));

        assertThat(Screen.of(shell).text())
                .contains("╭ New ticket")
                .contains("│ Describe: ")
                .contains("╰ enter start · esc cancel");
    }

    @Test
    void shouldShowCInTheKeyBarOnTheBoard() {
        Shell shell = shellCapturing(hosting, boards, Runnable::run, ticketView());

        assertThat(Screen.of(shell).lines().getLast()).contains("N new  c capture");
    }

    @Test
    void shouldNotOpenThePopupWhenTheBoardIsNotFocused() {
        Shell shell = shellCapturing(hosting, boards, Runnable::run, ticketView());
        shell.press(alt('j'));

        shell.press(key('c'));

        assertThat(Screen.of(shell).text()).doesNotContain("New ticket");
        assertThat(Screen.of(shell.render(), WIDE, HEIGHT).lines().getLast()).doesNotContain("c capture");
    }

    @Test
    void shouldAnswerEnterAndEscInTheKeyBarWhileOpen() {
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        assertThat(Screen.of(shell).lines().getLast()).contains("enter start · esc cancel").doesNotContain("q quit");
    }

    @Test
    void shouldTakeEveryKeyAsTextWhileOpen() {
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        typed(shell, "quit R");

        assertThat(Screen.of(shell).text()).contains("│ Describe: quit R");
        assertThat(shell.quitRequested()).isFalse();
        verifyNoInteractions(boards);
    }

    @Test
    void shouldDeleteBackwardAndMoveTheCursor() {
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        typed(shell, "Hxi");
        shell.press(KeyEvent.ofKey(KeyCode.LEFT));
        shell.press(KeyEvent.ofKey(KeyCode.BACKSPACE));

        Screen screen = Screen.of(shell);
        assertThat(screen.text()).contains("│ Describe: Hi");
        assertThat(screen.cursor()).hasValueSatisfying(at -> assertThat(screen.lines().get(at.y())).contains("Describe: Hi"));
    }

    @Test
    void shouldKeepTheEndOfADescriptionLongerThanThePopupAndTheCursorInIt() {
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        typed(shell, "a".repeat(60) + "END");

        Screen screen = Screen.of(shell);
        assertThat(screen.text()).contains("Describe: " + "a".repeat(33) + "END");
        assertThat(screen.cursor()).isPresent();
    }

    @Test
    void shouldStillQuitOnCtrlCWhileOpen() {
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        shell.press(ctrl('c'));

        assertThat(shell.quitRequested()).isTrue();
    }

    @Test
    void shouldStartTheSessionWithTheDescriptionTypedInTheProjectsRootOnEnter() {
        when(hosting.startNewTicket(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        typed(shell, "add a \"dark mode\"  toggle");
        shell.press(enter());

        assertThat(newTicketPayload(hosting).root()).isEqualTo(projectRoot().value());
        assertThat(newTicketPayload(hosting).description()).isEqualTo("add a \"dark mode\"  toggle");
    }

    @Test
    void shouldCloseThePopupAndFocusTheNewSession() {
        when(hosting.startNewTicket(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        typed(shell, "Hi");
        shell.press(enter());

        assertThat(Screen.of(shell).text()).doesNotContain("New ticket");
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldReadTheBoardNoMoreAndSelectNothingWhenTheSessionStarts() {
        when(hosting.startNewTicket(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());

        typed(shell, "Hi");
        shell.press(enter());

        verifyNoInteractions(boards);
    }

    @Test
    void shouldStartOffTheRenderThread() {
        List<Runnable> queued = new ArrayList<>();
        Shell shell = shellOpenForADescription(hosting, boards, queued::add, ticketView());
        typed(shell, "Hi");

        shell.press(enter());

        verify(hosting, never()).startNewTicket(any());
        assertThat(queued).hasSize(1);
    }

    @Test
    void shouldHoldThePopupStillWhileTheStartIsOut() {
        List<Runnable> queued = new ArrayList<>();
        Shell shell = shellOpenForADescription(hosting, boards, queued::add, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        typed(shell, "x");
        shell.press(enter());
        shell.press(escape());

        assertThat(Screen.of(shell).text()).contains("│ Describe: Hi ").contains("starting…").doesNotContain("Hix");
        assertThat(queued).hasSize(1);
    }

    @Test
    void shouldRefuseABlankDescriptionInThePopupAndStartNothing() {
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());
        typed(shell, "   ");

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("New ticket").contains("⚠ describe the ticket first");
        verifyNoInteractions(hosting);
    }

    @Test
    void shouldKeepThePopupWithTheDescriptionAndTheReasonWhenNoSessionStarted() {
        when(hosting.startNewTicket(any())).thenReturn(notStartedBecause("hosted session: claude is not on the PATH."));
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());
        typed(shell, "Hi");

        shell.press(enter());

        assertThat(Screen.of(shell).text())
                .contains("│ Describe: Hi")
                .contains("│ ⚠ hosted session: claude is not on the PATH.");
        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldKeepThePopupWhenTheStartThrows() {
        when(hosting.startNewTicket(any())).thenThrow(new IllegalStateException("pty exploded"));
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());
        typed(shell, "Hi");

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("│ Describe: Hi").contains("⚠ pty exploded");
    }

    @Test
    void shouldTryAgainOnEnterAfterANotStartedAnswer() {
        when(hosting.startNewTicket(any()))
                .thenReturn(notStartedBecause(reason().value()), new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        shell.press(enter());

        verify(hosting, times(2)).startNewTicket(any());
        assertThat(Screen.of(shell).text()).doesNotContain("New ticket");
    }

    @Test
    void shouldNeverRetryByItself() {
        List<Runnable> queued = new ArrayList<>();
        when(hosting.startNewTicket(any())).thenReturn(notStartedBecause(reason().value()));
        Shell shell = shellOpenForADescription(hosting, boards, queued::add, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        ShellFixture.drained(queued);
        ShellFixture.drained(queued);
        Screen.of(shell);

        verify(hosting, times(1)).startNewTicket(any());
        assertThat(queued).isEmpty();
    }

    @Test
    void shouldDropThePopupAndItsDescriptionOnEscAfterANotStartedAnswer() {
        when(hosting.startNewTicket(any())).thenReturn(notStartedBecause(reason().value()));
        Shell shell = shellOpenForADescription(hosting, boards, Runnable::run, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        shell.press(escape());
        shell.press(key('c'));

        assertThat(Screen.of(shell).text()).doesNotContain("Hi").doesNotContain("⚠").contains("│ Describe: ");
    }
}

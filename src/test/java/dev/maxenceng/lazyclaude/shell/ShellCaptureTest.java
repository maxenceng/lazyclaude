package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.projectRoot;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.capturedTicketViewNumbered;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.refusedTicketCaptureView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.shell.ShellCaptureFixture.shellCapturing;
import static dev.maxenceng.lazyclaude.shell.ShellCaptureFixture.shellOpenForATitle;
import static dev.maxenceng.lazyclaude.shell.ShellCaptureFixture.typed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.escape;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.readingBoard;
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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureCommand;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCapturePayload;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

class ShellCaptureTest {

    private static final int WIDE = 120;
    private static final int HEIGHT = 24;
    private static final TicketCapturePayload TITLED_HI = new TicketCapturePayload(projectRoot().value(), "Hi");

    private final TicketCaptureCommand capturing = mock(TicketCaptureCommand.class);
    private final BoardQuery boards = mock(BoardQuery.class);

    @Test
    void shouldOpenAOneLinePopupAskingForATitleWithCOnTheBoard() {
        Shell shell = shellCapturing(capturing, boards, Runnable::run, ticketView());

        shell.press(key('c'));

        assertThat(Screen.of(shell).text())
                .contains("╭ New ticket")
                .contains("│ Title: ")
                .contains("╰ enter create · esc cancel");
    }

    @Test
    void shouldShowCInTheKeyBarOnTheBoard() {
        Shell shell = shellCapturing(capturing, boards, Runnable::run, ticketView());

        assertThat(Screen.of(shell).lines().getLast()).contains("N new  c capture");
    }

    @Test
    void shouldNotOpenThePopupWhenTheBoardIsNotFocused() {
        Shell shell = shellCapturing(capturing, boards, Runnable::run, ticketView());
        shell.press(alt('j'));

        shell.press(key('c'));

        assertThat(Screen.of(shell).text()).doesNotContain("New ticket");
        assertThat(Screen.of(shell.render(), WIDE, HEIGHT).lines().getLast()).doesNotContain("c capture");
    }

    @Test
    void shouldAnswerEnterAndEscInTheKeyBarWhileOpen() {
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());

        assertThat(Screen.of(shell).lines().getLast()).contains("enter create · esc cancel").doesNotContain("q quit");
    }

    @Test
    void shouldTakeEveryKeyAsTextWhileOpen() {
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());

        typed(shell, "quit R");

        assertThat(Screen.of(shell).text()).contains("│ Title: quit R");
        assertThat(shell.quitRequested()).isFalse();
        verifyNoInteractions(boards);
    }

    @Test
    void shouldDeleteBackwardAndMoveTheCursor() {
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());

        typed(shell, "Hxi");
        shell.press(KeyEvent.ofKey(KeyCode.LEFT));
        shell.press(KeyEvent.ofKey(KeyCode.BACKSPACE));

        Screen screen = Screen.of(shell);
        assertThat(screen.text()).contains("│ Title: Hi");
        assertThat(screen.cursor()).hasValueSatisfying(at -> assertThat(screen.lines().get(at.y())).contains("Title: Hi"));
    }

    @Test
    void shouldKeepTheEndOfATitleLongerThanThePopupAndTheCursorInIt() {
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());

        typed(shell, "a".repeat(60) + "END");

        Screen screen = Screen.of(shell);
        assertThat(screen.text()).contains("Title: " + "a".repeat(36) + "END");
        assertThat(screen.cursor()).isPresent();
    }

    @Test
    void shouldStillQuitOnCtrlCWhileOpen() {
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());

        shell.press(ctrl('c'));

        assertThat(shell.quitRequested()).isTrue();
    }

    @Test
    void shouldCreateTheTicketWithTheTitleTypedInTheProjectsRootOnEnter() {
        when(capturing.capture(TITLED_HI)).thenReturn(capturedTicketViewNumbered(2));
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());

        typed(shell, "Hi");
        shell.press(enter());

        verify(capturing).capture(TITLED_HI);
    }

    @Test
    void shouldCloseThePopupAndSelectTheNewDraftByTheNumberGhReturned() {
        when(capturing.capture(TITLED_HI)).thenReturn(capturedTicketViewNumbered(7));
        BoardRead.returning(
                boards,
                boardViewOf(
                        ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT), ticketViewNumbered(7, Status.DRAFT)));
        Shell shell = shellOpenForATitle(
                capturing, boards, Runnable::run, ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT));

        typed(shell, "Hi");
        shell.press(enter());

        Screen screen = Screen.of(shell);
        assertThat(screen.text()).doesNotContain("New ticket");
        assertThat(screen.reversedTextOnLine(screen.positionInLeftColumn("#7").y())).contains("#7");
    }

    @Test
    void shouldNotSelectTheNewDraftOnALaterReadWhenTheReadAfterTheCaptureFailed() {
        when(capturing.capture(TITLED_HI)).thenReturn(capturedTicketViewNumbered(7));
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(7, Status.DRAFT))));
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketViewNumbered(1, Status.DRAFT));
        typed(shell, "Hi");

        shell.press(enter());
        shell.boardChanged();

        Screen screen = Screen.of(shell);
        assertThat(screen.boardPanelText()).contains("#7");
        assertThat(screen.reversedTextOnLine(screen.positionInLeftColumn("#7").y()))
                .as("the selection belongs to the read made at once after the create, not to whichever read lands later")
                .doesNotContain("#7");
    }

    @Test
    void shouldNotSelectTheNewDraftOnALaterReadWhenTheReadAfterTheCaptureLackedIt() {
        when(capturing.capture(TITLED_HI)).thenReturn(capturedTicketViewNumbered(7));
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT))))
                .thenReturn(Optional.of(boardViewOf(
                        ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT), ticketViewNumbered(7, Status.DRAFT))));
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketViewNumbered(1, Status.DRAFT));
        typed(shell, "Hi");

        shell.press(enter());
        Screen.of(shell);
        shell.boardChanged();

        Screen screen = Screen.of(shell);
        assertThat(screen.boardPanelText()).contains("#7");
        assertThat(screen.reversedTextOnLine(screen.positionInLeftColumn("#7").y()))
                .as("the selection belongs to the read made at once after the create, not to whichever read lands later")
                .doesNotContain("#7");
    }

    @Test
    void shouldAddNoRowBeforeTheBoardIsReadAgain() {
        List<Runnable> queued = new ArrayList<>();
        when(capturing.capture(TITLED_HI)).thenReturn(capturedTicketViewNumbered(7));
        BoardRead.returning(
                boards, boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(7, Status.DRAFT)));
        Shell shell = shellOpenForATitle(capturing, boards, queued::add, ticketViewNumbered(1, Status.DRAFT));
        typed(shell, "Hi");

        shell.press(enter());
        ShellFixture.drained(queued);

        assertThat(Screen.of(shell).boardPanelText()).doesNotContain("#7");
        verify(boards, never()).boardReadAgainFor(any());
        assertThat(queued).as("the read is queued at once, off the render thread").hasSize(1);
        ShellFixture.drained(queued);
        assertThat(Screen.of(shell).boardPanelText()).contains("#7");
    }

    @Test
    void shouldCreateOffTheRenderThread() {
        List<Runnable> queued = new ArrayList<>();
        Shell shell = shellOpenForATitle(capturing, boards, queued::add, ticketView());
        typed(shell, "Hi");

        shell.press(enter());

        verify(capturing, never()).capture(any());
        assertThat(queued).hasSize(1);
    }

    @Test
    void shouldHoldThePopupStillWhileTheCreateIsOut() {
        List<Runnable> queued = new ArrayList<>();
        Shell shell = shellOpenForATitle(capturing, boards, queued::add, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        typed(shell, "x");
        shell.press(enter());
        shell.press(escape());

        assertThat(Screen.of(shell).text()).contains("│ Title: Hi ").contains("creating…").doesNotContain("Hix");
        assertThat(queued).hasSize(1);
    }

    @Test
    void shouldKeepThePopupWithTheTitleAndTheReasonWhenTheCreateIsRefused() {
        when(capturing.capture(TITLED_HI)).thenReturn(refusedTicketCaptureView());
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());
        typed(shell, "Hi");

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("│ Title: Hi").contains("│ ⚠ " + reason().value());
        verify(boards, never()).boardReadAgainFor(any());
    }

    @Test
    void shouldKeepThePopupWhenTheCreateThrows() {
        when(capturing.capture(TITLED_HI)).thenThrow(new IllegalStateException("gh exploded"));
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());
        typed(shell, "Hi");

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("│ Title: Hi").contains("⚠ gh exploded");
    }

    @Test
    void shouldTryAgainOnEnterAfterARefusal() {
        when(capturing.capture(TITLED_HI)).thenReturn(refusedTicketCaptureView(), capturedTicketViewNumbered(2));
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        shell.press(enter());

        verify(capturing, times(2)).capture(TITLED_HI);
        assertThat(Screen.of(shell).text()).doesNotContain("New ticket");
    }

    @Test
    void shouldNeverRetryByItself() {
        List<Runnable> queued = new ArrayList<>();
        when(capturing.capture(TITLED_HI)).thenReturn(refusedTicketCaptureView());
        Shell shell = shellOpenForATitle(capturing, boards, queued::add, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        ShellFixture.drained(queued);
        ShellFixture.drained(queued);
        Screen.of(shell);

        verify(capturing, times(1)).capture(TITLED_HI);
        assertThat(queued).isEmpty();
    }

    @Test
    void shouldDropThePopupAndItsTitleOnEscAfterARefusal() {
        when(capturing.capture(TITLED_HI)).thenReturn(refusedTicketCaptureView());
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());
        typed(shell, "Hi");
        shell.press(enter());

        shell.press(escape());
        shell.press(key('c'));

        assertThat(Screen.of(shell).text()).doesNotContain("Hi").doesNotContain("⚠").contains("│ Title: ");
    }

    @Test
    void shouldAskTheCommandWithAnEmptyTitleSoTheDomainRefusesIt() {
        TicketCapturePayload empty = new TicketCapturePayload(projectRoot().value(), "");
        when(capturing.capture(empty)).thenReturn(refusedTicketCaptureView());
        Shell shell = shellOpenForATitle(capturing, boards, Runnable::run, ticketView());

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("New ticket").contains("⚠ " + reason().value());
    }
}

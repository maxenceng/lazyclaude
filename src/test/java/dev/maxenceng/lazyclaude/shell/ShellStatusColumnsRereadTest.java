package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.boardReadAgainAs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellZoomedOnTheSecondDraft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.published.Status;

class ShellStatusColumnsRereadTest {

    private BoardQuery boards;

    @BeforeEach
    void createQueries() {
        boards = mock(BoardQuery.class);
    }

    @Test
    void shouldRedrawTheColumnsWithTheBoardReadAgain() {
        boardReadAgainAs(boards, boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT), ticketViewNumbered(7, Status.TODO)));
        Shell shell = shellZoomedOnTheSecondDraft(boards);

        shell.boardChanged();

        assertThat(Screen.of(shell).text()).contains("todo (1)", "PROJ-7").doesNotContain("Sessions");
    }

    @Test
    void shouldKeepTheCursorOnItsTicketWhenARowAppearsAboveIt() {
        boardReadAgainAs(boards, boardViewOf(
                ticketViewNumbered(0, Status.DRAFT),
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT)));
        Shell shell = shellZoomedOnTheSecondDraft(boards);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).anySatisfy(text -> assertThat(text).contains("PROJ-2"));
    }

    @Test
    void shouldFollowTheTicketToTheColumnItMovedTo() {
        boardReadAgainAs(boards, boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO), ticketViewNumbered(3, Status.DRAFT)));
        Shell shell = shellZoomedOnTheSecondDraft(boards);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).anySatisfy(text -> assertThat(text).contains("PROJ-2"));
    }

    @Test
    void shouldLeaveTheCursorOnTheRowThatTookTheVanishedTicketsPlace() {
        boardReadAgainAs(boards, boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(3, Status.DRAFT)));
        Shell shell = shellZoomedOnTheSecondDraft(boards);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).anySatisfy(text -> assertThat(text).contains("PROJ-3"));
    }

    @Test
    void shouldLeaveTheCursorOnTheLastRowWhenTheVanishedTicketWasTheLast() {
        boardReadAgainAs(boards, boardViewOf(ticketViewNumbered(1, Status.DRAFT)));
        Shell shell = shellZoomedOnTheSecondDraft(boards);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).anySatisfy(text -> assertThat(text).contains("PROJ-1"));
    }

    @Test
    void shouldMoveTheCursorToTheNextColumnWhenTheVanishedTicketsColumnEmptied() {
        boardReadAgainAs(boards, boardViewOf(ticketViewNumbered(8, Status.IN_PROGRESS)));
        Shell shell = shellZoomedOnTheSecondDraft(boards);

        shell.boardChanged();
        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("PROJ-8 · in-progress");
    }

    @Test
    void shouldKeepTheColumnsOpenWhenTheBoardEmptiesOut() {
        boardReadAgainAs(boards, boardViewOf());
        Shell shell = shellZoomedOnTheSecondDraft(boards);

        shell.boardChanged();

        assertThat(Screen.of(shell).text()).contains("draft (0)").doesNotContain("Sessions");
    }
}

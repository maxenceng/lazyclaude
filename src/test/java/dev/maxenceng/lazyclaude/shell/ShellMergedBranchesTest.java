package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.secondTitle;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedPullRequestsViewOn;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.noMergedPullRequestsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.readingBoardWithMerged;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellReading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.published.Status;

class ShellMergedBranchesTest {

    private BoardQuery boards;

    @BeforeEach
    void createBoardQuery() {
        boards = mock(BoardQuery.class);
    }

    @Test
    void shouldReadTheBoardAgainWhenATicketBranchBecomesMerged() {
        when(boards.refreshedBoardFor(readingBoardWithMerged("PROJ-1")))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().title(secondTitle()).build())));
        Shell shell = shellReading(boards, ticketView());

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));

        assertThat(Screen.of(shell).boardPanelText()).contains("PROJ-1 Plan the next").doesNotContain("PROJ-1 A board");
    }

    @Test
    void shouldReadTheBoardOnTheFetchingThreadBeforeAnyFrameIsDrawn() {
        when(boards.refreshedBoardFor(readingBoardWithMerged("PROJ-1"))).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellReading(boards, ticketView());

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));

        verify(boards).refreshedBoardFor(readingBoardWithMerged("PROJ-1"));
    }

    @Test
    void shouldKeepTheSelectionOnTheSameTicketWhenItsRowMoves() {
        when(boards.refreshedBoardFor(readingBoardWithMerged("PROJ-1")))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().status(Status.IN_PROGRESS).build(), secondTicketView())));
        Shell shell = shellReading(boards, ticketView(), secondTicketView());
        shell.press(key('j'));
        shell.press(key('j'));

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("PROJ-2");
    }

    @Test
    void shouldNotReadTheBoardWhileNoPullRequestIsKnownMerged() {
        Shell shell = shellReading(boards, ticketView());

        shell.mergedPullRequestsChanged(noMergedPullRequestsView());
        Screen.of(shell);

        verifyNoInteractions(boards);
    }

    @Test
    void shouldNotReadTheBoardForAMergedBranchThatIsNoTicketsBranch() {
        Shell shell = shellReading(boards, ticketView());

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("close-PROJ-1", "PROJ-10-another"));
        Screen.of(shell);

        verifyNoInteractions(boards);
    }

    @Test
    void shouldNotReadTheBoardAgainWhenTheMergedTicketBranchesAreTheSame() {
        when(boards.refreshedBoardFor(readingBoardWithMerged("PROJ-1"))).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellReading(boards, ticketView());

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));
        Screen.of(shell);
        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("other", "PROJ-1"));
        Screen.of(shell);

        verify(boards, times(1)).refreshedBoardFor(readingBoardWithMerged("PROJ-1"));
    }

    @Test
    void shouldReadTheBoardAgainWhenGitHubStopsAnsweringAndTheBranchesCountAsSourcesAgain() {
        when(boards.refreshedBoardFor(readingBoardWithMerged("PROJ-1"))).thenReturn(Optional.of(boardViewOf(ticketView())));
        when(boards.refreshedBoardFor(readingBoardWithMerged())).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellReading(boards, ticketView());
        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));
        Screen.of(shell);

        shell.mergedPullRequestsChanged(noMergedPullRequestsView());
        Screen.of(shell);

        verify(boards).refreshedBoardFor(readingBoardWithMerged());
    }

    @Test
    void shouldKeepTheBoardOnScreenAndTryAgainAtTheNextFetchWhenItCannotBeRead() {
        BoardPayload merged = readingBoardWithMerged("PROJ-1");
        when(boards.refreshedBoardFor(merged))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().title(secondTitle()).build())));
        Shell shell = shellReading(boards, ticketView());

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));
        assertThat(Screen.of(shell).boardPanelText()).contains("PROJ-1 A board");
        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));

        assertThat(Screen.of(shell).boardPanelText()).contains("PROJ-1 Plan the next");
    }
}

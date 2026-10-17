package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.projectRoot;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.secondTitle;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsViewOfDocuments;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.longDocumentView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedPullRequestsViewOn;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.readingBoardWithMerged;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellReadingBoardOnDocs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellReadingBoardWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

class ShellBoardWatchTest {

    private BoardQuery boards;
    private SessionQuery screens;

    @BeforeEach
    void createQueries() {
        boards = mock(BoardQuery.class);
        screens = blankScreens();
    }


    @Test
    void shouldReadTheBoardAgainWhenTheBoardChanges() {
        when(boards.boardReadAgainFor(readingBoardWithMerged()))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().title(secondTitle()).build())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        assertThat(Screen.of(shell).boardPanelText()).contains("PROJ-1 Plan the next").doesNotContain("PROJ-1 A board");
    }

    @Test
    void shouldReadTheBoardAgainWithTheBranchesKnownMergedAtTheTime() {
        when(boards.boardReadAgainFor(readingBoardWithMerged("PROJ-1"))).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());
        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1", "other"));

        shell.boardChanged();

        verify(boards, times(2)).boardReadAgainFor(readingBoardWithMerged("PROJ-1"));
    }

    @Test
    void shouldKeepTheBoardOnScreenWhenItCannotBeReadAgain() {
        when(boards.boardReadAgainFor(readingBoardWithMerged())).thenReturn(Optional.empty());
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        assertThat(Screen.of(shell).boardPanelText()).contains("PROJ-1 A board");
    }

    @Test
    void shouldKeepTheSelectionOnTheTicketWhenARowAppearsAboveIt() {
        when(boards.boardReadAgainFor(readingBoardWithMerged()))
                .thenReturn(Optional.of(boardViewOf(ticketView(), secondTicketView())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, secondTicketView()), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("PROJ-2");
    }

    @Test
    void shouldLeaveTheCursorOnTheRowThatTookThePlaceOfATicketThatVanished() {
        when(boards.boardReadAgainFor(readingBoardWithMerged())).thenReturn(Optional.of(boardViewOf(secondTicketView())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketView(), secondTicketView()), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("PROJ-2");
    }

    @Test
    void shouldLeaveTheCursorOnTheRowThatTookThePlaceOfATicketMovedIntoAFoldedGroup() {
        when(boards.boardReadAgainFor(readingBoardWithMerged()))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().status(Status.DONE).build(), secondTicketView())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketView(), secondTicketView()), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText())
                .singleElement()
                .asString()
                .as("the done group is folded, and unfolding it to follow the ticket would change a fold nobody asked to change")
                .contains("PROJ-2");
        assertThat(Screen.of(shell).boardPanelText()).doesNotContain("PROJ-1 A board");
    }

    @Test
    void shouldRedrawTheOpenTicketDetailAndKeepItsScrollPosition() {
        when(boards.boardReadAgainFor(readingBoardWithMerged()))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder()
                        .title(secondTitle())
                        .body(longTicketView().body())
                        .build())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, longTicketView()), key('j'), 1);
        Screen.of(shell);
        shell.press(key('l'));
        pressed(shell, key('j'), 3);
        String scrolledTo = Screen.of(shell).mainViewRows().getFirst();

        shell.boardChanged();

        Screen after = Screen.of(shell);
        assertThat(after.mainViewRows().getFirst()).as("the scroll position stays").isEqualTo(scrolledTo);
        assertThat(after.boardPanelText()).contains("Plan the next");
    }

    @Test
    void shouldKeepTheScrollPositionOfAnOpenDocumentWhenTheBoardCursorMoves() {
        when(boards.boardReadAgainFor(readingBoardWithMerged())).thenReturn(Optional.of(boardViewOf(secondTicketView())));
        Shell shell = pressed(
                shellReadingBoardOnDocs(boards, docsViewOfDocuments(longDocumentView()), ticketView(), secondTicketView()),
                key('j'),
                1);
        pressed(shell, alt('j'), 2);
        Screen.of(shell);
        shell.press(key('l'));
        pressed(shell, key('j'), 3);
        Screen.of(shell);

        shell.boardChanged();

        assertThat(Screen.of(shell).mainViewRows().getFirst())
                .as("the board's cursor moved, but the main view shows a document, whose reader did not move")
                .contains("Row 04");
    }

    @Test
    void shouldHandSessionsTheTicketIdsOfTheBoardJustRead() {
        when(boards.boardReadAgainFor(readingBoardWithMerged()))
                .thenReturn(Optional.of(boardViewOf(ticketView(), secondTicketView())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        verify(screens)
                .knowTickets(new SessionPayload(
                        projectRoot().value(), List.of(new LinkedTicketId("PROJ-1"), new LinkedTicketId("PROJ-2"))));
    }

    @Test
    void shouldNotHandSessionsTheTicketIdsAgainWhenTheyAreTheSame() {
        when(boards.boardReadAgainFor(readingBoardWithMerged())).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        verify(screens, never()).knowTickets(any());
    }
}

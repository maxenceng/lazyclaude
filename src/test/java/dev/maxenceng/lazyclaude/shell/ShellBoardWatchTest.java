package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.projectRoot;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.secondTitle;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsViewOfDocuments;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.longDocumentView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.readingBoard;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellReadingBoardOnDocs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellReadingBoardWith;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellReadingUnavailableBoard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.published.Body;
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
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().title(secondTitle()).build())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        assertThat(Screen.of(shell).boardPanelText()).contains("#1 Plan the next").doesNotContain("#1 A board");
    }

    @Test
    void shouldKeepTheBoardOnScreenWhenItCannotBeReadAgain() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.empty());
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        assertThat(Screen.of(shell).boardPanelText()).contains("#1 A board");
    }

    @Test
    void shouldKeepTheSelectionOnTheTicketWhenARowAppearsAboveIt() {
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketView(), secondTicketView())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, secondTicketView()), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#2");
    }

    @Test
    void shouldLeaveTheCursorOnTheRowThatTookThePlaceOfATicketThatVanished() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(secondTicketView())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketView(), secondTicketView()), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#2");
    }

    @Test
    void shouldLeaveTheCursorOnTheRowThatTookThePlaceOfATicketMovedIntoAFoldedGroup() {
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().status(Status.DONE).build(), secondTicketView())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketView(), secondTicketView()), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText())
                .singleElement()
                .asString()
                .as("the done group is folded, and unfolding it to follow the ticket would change a fold nobody asked to change")
                .contains("#2");
        assertThat(Screen.of(shell).boardPanelText()).doesNotContain("#1 A board");
    }

    @Test
    void shouldRedrawTheOpenTicketDetailAndKeepItsScrollPosition() {
        when(boards.boardReadAgainFor(readingBoard()))
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
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(secondTicketView())));
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
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketView(), secondTicketView())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        verify(screens)
                .knowTickets(new SessionPayload(
                        projectRoot().value(), List.of(new LinkedTicketId("1"), new LinkedTicketId("2"))));
    }

    @Test
    void shouldNotHandSessionsTheTicketIdsAgainWhenTheyAreTheSame() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());

        shell.boardChanged();

        verify(screens, never()).knowTickets(any());
    }

    @Test
    void shouldFollowTheSelectedTicketIntoTheStatusGroupItMovedTo() {
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().status(Status.TODO).build(), secondTicketView())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketView(), secondTicketView()), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#1");
        assertThat(Screen.of(shell).boardPanelText()).containsSubsequence("▾ draft (1)", "#2", "▾ todo (1)", "#1");
    }

    @Test
    void shouldLeaveTheCursorOnTheNextRowOfTheOldGroupWhenTheSelectedTicketIsGone() {
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(3, Status.DRAFT), ticketViewNumbered(4, Status.TODO))));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT), ticketViewNumbered(3, Status.DRAFT), ticketViewNumbered(4, Status.TODO)), key('j'), 2);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#3");
    }

    @Test
    void shouldLeaveTheCursorOnThePreviousRowOfTheOldGroupWhenTheSelectedTicketWasItsLast() {
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(4, Status.TODO))));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT), ticketViewNumbered(4, Status.TODO)), key('j'), 2);

        shell.boardChanged();

        assertThat(Screen.of(shell).reversedText())
                .as("the next row on the board is the todo header, which is not a row of the group the ticket left")
                .singleElement()
                .asString()
                .contains("#1");
    }

    @Test
    void shouldLeaveTheCursorOnTheHeaderOfTheOldGroupWhenTheSelectedTicketWasItsOnlyOne() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(ticketViewNumbered(4, Status.TODO))));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(4, Status.TODO)), key('j'), 1);

        shell.boardChanged();

        assertThat(Screen.of(shell).boardPanelLines()).anySatisfy(line -> assertThat(line).contains("▾ draft (0)"));
        assertThat(Screen.of(shell).reversedText()).noneSatisfy(text -> assertThat(text).contains("#4"));
    }

    @Test
    void shouldClampTheScrollOfTheOpenTicketDetailWhenTheBodyShrank() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(ticketViewBuilder().body(new Body("Short.")).build())));
        Shell shell = pressed(shellReadingBoardWith(boards, screens, longTicketView()), key('j'), 1);
        Screen.of(shell);
        shell.press(key('l'));
        shell.press(key('G'));
        Screen.of(shell);

        shell.boardChanged();

        assertThat(Screen.of(shell).mainViewRows().getFirst()).as("the whole shorter ticket shows from its facts line").startsWith("#1 · draft");
        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("Short."));
    }

    @Test
    void shouldLeaveAFoldedGroupFoldedWhenTheBoardIsReadAgain() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(ticketView(), secondTicketView())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());
        shell.press(enter());

        shell.boardChanged();

        assertThat(Screen.of(shell).boardPanelText()).contains("▸ draft (2)").doesNotContain("#1 A board", "#2");
    }

    @Test
    void shouldKeepTheZoomWhenTheBoardIsReadAgain() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(ticketView(), secondTicketView())));
        Shell shell = shellReadingBoardWith(boards, screens, ticketView());
        shell.press(key('z'));

        shell.boardChanged();

        assertThat(Screen.of(shell).text()).as("the status columns are still across the screen").contains("draft (2)", "todo (0)").doesNotContain("Sessions");
    }

    @Test
    void shouldReplaceTheReasonWithTheTicketsWhenTheBoardIsReadAtLast() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellReadingUnavailableBoard(boards, screens);
        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("gh is not logged in; run gh auth login"));

        shell.boardChanged();

        assertThat(Screen.of(shell).boardPanelText()).contains("▾ draft (1)", "#1");
        assertThat(Screen.of(shell).mainViewRows()).noneSatisfy(row -> assertThat(row).contains("gh auth login"));
    }
}

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.nestedDocsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.leftPress;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.newShell;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOn;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnDocs;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.tamboui.layout.Position;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

class ShellMouseTest {

    @Test
    void shouldSelectABoardRowWithAClick() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT));

        assertThat(shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("#2")))).isTrue();

        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("#2"));
    }

    @Test
    void shouldFocusThePanelOfTheRowClicked() {
        Shell shell = newShell(boardViewOf(ticketViewNumbered(1, Status.DRAFT)), docsView(), emptySessionsView());

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("adr")));

        assertThat(shell.focus()).isEqualTo(Focus.DOCS);
        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("adr"));
    }

    @Test
    void shouldLetTheMainViewFollowTheSelectedRowAsItDoesForJAndK() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("#2")));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("#2 · draft");
    }

    @Test
    void shouldNotFoldAHeaderOnTheClickThatSelectsIt() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("#1")));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("draft")));

        assertThat(Screen.of(shell).boardPanelText()).contains("▾ draft (1)", "#1");
    }

    @Test
    void shouldFoldAHeaderOnASecondClick() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("#1")));
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("draft")));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("draft")));

        assertThat(Screen.of(shell).boardPanelText()).contains("▸ draft (1)").doesNotContain("#1");
    }

    @Test
    void shouldFocusSessionsOnASecondClickOnATicketRow() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("#1")));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("#1")));

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
    }

    @Test
    void shouldOnlyFocusAnUnfocusedPanelOnAClickOnItsSelectedRow() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        shell.press(alt('j'));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("draft")));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).boardPanelText()).contains("▾ draft (1)", "#1");
    }

    @Test
    void shouldFoldADocsFolderOnASecondClick() {
        Shell shell = newShell(boardViewOf(ticketViewNumbered(1, Status.DRAFT)), docsView(), emptySessionsView());
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("adr")));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("adr")));

        assertThat(Screen.of(shell).boardPanelText()).contains("▸ adr");
    }

    @Test
    void shouldSelectTheFirstRowDrawnOfAScrolledList() {
        Shell shell = pressed(shellOn(ticketViewsIn(Status.TODO, 30)), key('j'), 100);
        Screen scrolled = Screen.of(shell);
        String first = scrolled.boardPanelLines().getFirst().strip();

        shell.mouse(leftPress(scrolled.positionInLeftColumn(first)));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().isEqualTo(first);
    }

    @Test
    void shouldSelectTheRowAboveTheLastRowDrawnOfAScrolledList() {
        Shell shell = pressed(shellOn(ticketViewsIn(Status.TODO, 30)), key('j'), 100);

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("#30")));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#30");
    }

    @Test
    void shouldSelectTheLastRowDrawnAndNotTheBorderBelowIt() {
        Shell shell = shellOn(ticketViewsIn(Status.TODO, 30));
        Position last = Screen.of(shell).positionInLeftColumn("#8");

        assertThat(shell.mouse(leftPress(new Position(last.x(), last.y() + 1)))).isFalse();
        assertThat(shell.mouse(leftPress(last))).isTrue();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#8");
    }

    @Test
    void shouldNeitherSelectNorFoldWhenAClickLandsBelowTheLastRowOfTheFocusedPanel() {
        Shell shell = shellOnDocs(nestedDocsView());
        Position first = Screen.of(shell).positionInLeftColumn("adr");

        assertThat(shell.mouse(leftPress(new Position(first.x(), first.y() + 5)))).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.DOCS);
        assertThat(Screen.of(shell).boardPanelText()).contains("▾ adr", "superseded");
    }

    @Test
    void shouldFocusAnEmptyPanelClickedAnywhereInside() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        Position inside = Screen.of(shell).positionInLeftColumn("no live session");

        assertThat(shell.mouse(leftPress(inside))).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
    }

    @Test
    void shouldIgnoreAClickOnAPanelBorder() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));

        assertThat(shell.mouse(leftPress(new Position(0, Screen.of(shell).lineOf("#1"))))).isFalse();
    }

    @Test
    void shouldIgnoreARightClickAndAMiddleClickOnARow() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT));
        shell.press(alt('j'));
        Position row = Screen.of(shell).positionInLeftColumn("#2");

        assertThat(shell.mouse(MouseEvent.press(MouseButton.RIGHT, row.x(), row.y()))).isFalse();
        assertThat(shell.mouse(MouseEvent.press(MouseButton.MIDDLE, row.x(), row.y()))).isFalse();

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        assertThat(Screen.of(shell).reversedText()).noneSatisfy(text -> assertThat(text).contains("#2"));
    }

    @Test
    void shouldIgnoreAReleaseAndADragOnARow() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT));
        Position row = Screen.of(shell).positionInLeftColumn("#2");

        assertThat(shell.mouse(MouseEvent.release(MouseButton.LEFT, row.x(), row.y()))).isFalse();
        assertThat(shell.mouse(MouseEvent.drag(MouseButton.LEFT, row.x(), row.y()))).isFalse();

        assertThat(Screen.of(shell).reversedText()).noneSatisfy(text -> assertThat(text).contains("#2"));
    }

    @Test
    void shouldIgnoreAClickOnTheStatusLineAndOnTheKeyBar() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        shell.press(alt('j'));
        Screen screen = Screen.of(shell);

        assertThat(shell.mouse(leftPress(new Position(2, 0)))).isFalse();
        assertThat(shell.mouse(leftPress(new Position(2, screen.lines().size() - 1)))).isFalse();

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
    }

    @Test
    void shouldReachNothingBeforeAFrameWasDrawn() {
        assertThat(newShell(boardViewOf(ticketViewNumbered(1, Status.DRAFT)), emptyDocsView(), emptySessionsView())
                        .mouse(leftPress(new Position(2, 2))))
                .isFalse();
    }
}

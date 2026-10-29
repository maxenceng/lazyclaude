package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.leftPress;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.newShell;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOn;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellReviewing;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Body;
import dev.tamboui.layout.Position;
import dev.tamboui.tui.event.MouseEvent;

class ShellMouseMainViewTest {

    private static final Position IN_THE_MAIN_VIEW = new Position(40, 4);

    @Test
    void shouldFocusTheMainViewWithAClickInsideIt() {
        Shell shell = shellOn(ticketView());
        Screen.of(shell);

        assertThat(shell.mouse(leftPress(IN_THE_MAIN_VIEW))).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
    }

    @Test
    void shouldNotFocusTheMainViewWithAClickOnItsBorder() {
        Shell shell = shellOn(ticketView());
        Screen.of(shell);

        assertThat(shell.mouse(leftPress(new Position(28, 4)))).isFalse();

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldOnlyFocusTheMainViewWithAClickOnALinkInTheTicketDetail() {
        Shell shell = shellOn(ticketViewBuilder().body(new Body("See [the design](https://example.test/design) for more.")).build());
        shell.press(key('j'));
        Screen detail = Screen.of(shell);

        shell.mouse(leftPress(detail.positionOf("the design")));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).text()).contains("the design");
    }

    @Test
    void shouldScrollThreeLinesPerWheelNotchInTheMainViewAndFocusIt() {
        Shell shell = pressed(shellOn(longTicketView()), key('j'), 1);
        List<String> before = Screen.of(shell).mainViewRows();

        shell.mouse(MouseEvent.scrollDown(IN_THE_MAIN_VIEW.x(), IN_THE_MAIN_VIEW.y()));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).mainViewRows().getFirst()).isEqualTo(before.get(3));
    }

    @Test
    void shouldScrollBackUpThreeLinesWithTheWheel() {
        Shell shell = pressed(shellOn(longTicketView()), key('j'), 1);
        List<String> before = Screen.of(shell).mainViewRows();
        shell.mouse(MouseEvent.scrollDown(IN_THE_MAIN_VIEW.x(), IN_THE_MAIN_VIEW.y()));
        Screen.of(shell);

        shell.mouse(MouseEvent.scrollUp(IN_THE_MAIN_VIEW.x(), IN_THE_MAIN_VIEW.y()));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).isEqualTo(before.getFirst());
    }

    @Test
    void shouldMoveTheSelectionOneRowPerWheelNotchInAListAndFocusItsPanel() {
        Shell shell = shellOn(ticketView());
        shell.press(alt('j'));
        Position board = Screen.of(shell).positionInLeftColumn("draft");

        shell.mouse(MouseEvent.scrollDown(board.x(), board.y()));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("PROJ-1");
    }

    @Test
    void shouldMoveTheSelectionBackUpOneRowPerWheelNotch() {
        Shell shell = shellOn(ticketView());
        shell.press(key('j'));
        Position board = Screen.of(shell).positionInLeftColumn("PROJ-1");

        shell.mouse(MouseEvent.scrollUp(board.x(), board.y()));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("draft");
    }

    @Test
    void shouldIgnoreAHorizontalWheelNotch() {
        Shell shell = shellOn(ticketView());
        Position board = Screen.of(shell).positionInLeftColumn("draft");

        assertThat(shell.mouse(MouseEvent.scrollRight(board.x(), board.y()))).isFalse();
    }

    @Test
    void shouldLeaveTheFilesCommentsAndDiffOfAReviewToTheKeys() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());
        Screen review = Screen.of(shell);

        assertThat(shell.mouse(leftPress(review.positionInLeftColumn("Board")))).isFalse();
        assertThat(shell.mouse(leftPress(review.positionOf("── src/Board.java")))).isFalse();
        assertThat(shell.mouse(MouseEvent.scrollDown(IN_THE_MAIN_VIEW.x(), IN_THE_MAIN_VIEW.y()))).isFalse();

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
    }

    @Test
    void shouldMoveTheSelectionOneRowPerWheelNotchInSessionsAndDocs() {
        Shell shell = newShell(boardViewOf(ticketView()), docsView(), emptySessionsView());
        Position docsRow = Screen.of(shell).positionInLeftColumn("context-map.md");

        shell.mouse(MouseEvent.scrollDown(docsRow.x(), docsRow.y()));

        assertThat(shell.focus()).isEqualTo(Focus.DOCS);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("glossary.md");
    }
}

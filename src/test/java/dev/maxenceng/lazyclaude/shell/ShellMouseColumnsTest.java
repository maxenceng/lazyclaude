package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewsIn;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.leftPress;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellZoomedOnColumns;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.tamboui.layout.Position;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

class ShellMouseColumnsTest {

    @Test
    void shouldMoveTheCursorToTheTicketClickedInAnotherColumn() {
        Shell shell = shellZoomedOnColumns(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO));

        assertThat(shell.mouse(leftPress(Screen.of(shell).positionOf("#2")))).isTrue();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#2");
    }

    @Test
    void shouldPickTheTicketOnASecondClickAndLeaveTheZoom() {
        Shell shell = shellZoomedOnColumns(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO));
        shell.mouse(leftPress(Screen.of(shell).positionOf("#2")));

        shell.mouse(leftPress(Screen.of(shell).positionOf("#2")));

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        assertThat(Screen.of(shell).text()).contains("╭Board", "╭Sessions");
    }

    @Test
    void shouldNotPickTheTicketOnTheClickThatMovesTheCursorToIt() {
        Shell shell = shellZoomedOnColumns(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO));

        shell.mouse(leftPress(Screen.of(shell).positionOf("#2")));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).doesNotContain("╭Sessions");
    }

    @Test
    void shouldSelectTheRowsDrawnFromTheTopOfAColumn() {
        Shell shell = shellZoomedOnColumns(ticketViewsIn(Status.TODO, 30));
        Screen drawn = Screen.of(shell);
        Position first = drawn.positionOf("#1 ");

        shell.mouse(leftPress(new Position(first.x(), first.y() + 4)));
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#5");
        shell.mouse(leftPress(first));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#1 ");
    }

    @Test
    void shouldSelectTheFirstRowDrawnOfAColumnScrolledToItsEnd() {
        Shell shell = pressed(shellZoomedOnColumns(ticketViewsIn(Status.TODO, 30)), key('j'), 29);
        Screen scrolled = Screen.of(shell);
        int line = scrolled.lineOf("todo (30)") + 1;
        Matcher firstDrawn = Pattern.compile("#\\d+").matcher(scrolled.lines().get(line));
        assertThat(firstDrawn.find()).isTrue();

        shell.mouse(leftPress(new Position(firstDrawn.start(), line)));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains(firstDrawn.group());
    }

    @Test
    void shouldIgnoreAClickOnTheLineThatCountsTheRowsBelow() {
        Shell shell = shellZoomedOnColumns(ticketViewsIn(Status.TODO, 30));
        Screen drawn = Screen.of(shell);

        assertThat(shell.mouse(leftPress(drawn.positionOf("↓")))).isFalse();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#1 ");
    }

    @Test
    void shouldIgnoreAClickInTheNarrowColumnOfAFinishedStatus() {
        Shell shell = shellZoomedOnColumns(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(9, Status.DONE));
        Position title = Screen.of(shell).positionOf("done 1");

        assertThat(shell.mouse(leftPress(new Position(title.x() + 1, title.y() + 1)))).isFalse();
        assertThat(shell.mouse(leftPress(title))).isFalse();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#1 ");
    }

    @Test
    void shouldMoveTheCursorOneTicketPerWheelNotchWithinTheColumn() {
        Shell shell = shellZoomedOnColumns(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT));
        Position first = Screen.of(shell).positionOf("#1 ");

        shell.mouse(MouseEvent.scrollDown(first.x(), first.y()));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#2");
    }

    @Test
    void shouldMoveTheCursorIntoTheColumnUnderThePointerOnTheFirstWheelNotch() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO), ticketViewNumbered(3, Status.TODO));
        Position other = Screen.of(shell).positionOf("#3");

        shell.mouse(MouseEvent.scrollDown(other.x(), other.y()));
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#2");
        shell.mouse(MouseEvent.scrollDown(other.x(), other.y()));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#3");
    }

    @Test
    void shouldKeepTheCursorOnAWheelOverAnEmptyColumn() {
        Shell shell = shellZoomedOnColumns(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT));
        Position empty = Screen.of(shell).positionOf("todo (0)");

        shell.mouse(MouseEvent.scrollDown(empty.x(), empty.y() + 1));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#1 ");
    }

    @Test
    void shouldIgnoreARightClickOnATicket() {
        Shell shell = shellZoomedOnColumns(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO));
        Position row = Screen.of(shell).positionOf("#2");

        assertThat(shell.mouse(MouseEvent.press(MouseButton.RIGHT, row.x(), row.y()))).isFalse();

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("#1 ");
    }
}

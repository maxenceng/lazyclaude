package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketIdNumbered;
import static dev.maxenceng.lazyclaude.shell.StatusColumnsFixture.columnsOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Status;

class StatusColumnsTest {

    @Test
    void shouldHaveNoFirstTicketWhenTheBoardIsEmpty() {
        assertThat(columnsOf().first()).isEmpty();
    }

    @Test
    void shouldStartOnTheFirstTicketOfTheFirstColumnThatHoldsOne() {
        assertThat(columnsOf(ticketViewNumbered(5, Status.TODO), ticketViewNumbered(6, Status.TODO), ticketViewNumbered(7, Status.IN_REVIEW))
                        .first()
                        .map(ticket -> ticket.id().value()))
                .contains("5");
    }

    @Test
    void shouldStayOnTheLastRowOfAColumnWhenMovingDown() {
        StatusColumns columns = columnsOf(ticketViewNumbered(5, Status.TODO), ticketViewNumbered(6, Status.TODO));

        assertThat(columns.down(ticketIdNumbered(5)).map(ticket -> ticket.id().value())).contains("6");
        assertThat(columns.down(ticketIdNumbered(6)).map(ticket -> ticket.id().value())).contains("6");
    }

    @Test
    void shouldStayOnTheFirstRowOfAColumnWhenMovingUp() {
        StatusColumns columns = columnsOf(ticketViewNumbered(5, Status.TODO), ticketViewNumbered(6, Status.TODO));

        assertThat(columns.up(ticketIdNumbered(6)).map(ticket -> ticket.id().value())).contains("5");
        assertThat(columns.up(ticketIdNumbered(5)).map(ticket -> ticket.id().value())).contains("5");
    }

    @Test
    void shouldMoveToTheNextColumnThatHoldsATicketKeepingTheRowClamped() {
        StatusColumns columns = columnsOf(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(8, Status.IN_PROGRESS));

        assertThat(columns.right(ticketIdNumbered(3)).map(ticket -> ticket.id().value()))
                .as("todo is empty, so in-progress is next, and its only row takes the clamped index")
                .contains("8");
        assertThat(columns.left(ticketIdNumbered(8)).map(ticket -> ticket.id().value())).contains("1");
    }

    @Test
    void shouldWheelIntoAnotherColumnAtTheCursorsRowClamped() {
        StatusColumns columns = columnsOf(
                ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.DRAFT), ticketViewNumbered(5, Status.TODO));

        assertThat(columns.wheeled(ticketIdNumbered(2), 1, true).map(ticket -> ticket.id().value()))
                .as("the first notch over todo lands in it, on its only row, without moving along it")
                .contains("5");
        assertThat(columns.wheeled(ticketIdNumbered(5), 0, false).map(ticket -> ticket.id().value())).contains("1");
    }

    @Test
    void shouldWheelAlongTheColumnTheCursorStandsIn() {
        StatusColumns columns = columnsOf(ticketViewNumbered(5, Status.TODO), ticketViewNumbered(6, Status.TODO));

        assertThat(columns.wheeled(ticketIdNumbered(5), 1, true).map(ticket -> ticket.id().value())).contains("6");
        assertThat(columns.wheeled(ticketIdNumbered(6), 1, false).map(ticket -> ticket.id().value())).contains("5");
    }

    @Test
    void shouldKeepTheCursorWhenWheeledOverAnEmptyColumn() {
        StatusColumns columns = columnsOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(8, Status.IN_PROGRESS));

        assertThat(columns.wheeled(ticketIdNumbered(8), 1, true).map(ticket -> ticket.id().value())).contains("8");
    }

    @Test
    void shouldStayPutWhenNoColumnBeyondHoldsATicket() {
        StatusColumns columns = columnsOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(9, Status.DONE));

        assertThat(columns.right(ticketIdNumbered(1)).map(ticket -> ticket.id().value()))
                .as("done is folded, so the cursor never stands in it")
                .contains("1");
        assertThat(columns.left(ticketIdNumbered(1)).map(ticket -> ticket.id().value())).contains("1");
    }

    @Test
    void shouldFallBackToTheFirstTicketWhenTheCursorStandsOnANarrowColumnsTicket() {
        StatusColumns columns = columnsOf(ticketViewNumbered(1, Status.TODO), ticketViewNumbered(9, Status.DONE));

        assertThat(columns.down(ticketIdNumbered(9)).map(ticket -> ticket.id().value())).contains("1");
    }

    @Test
    void shouldLandOnTheRowThatTookTheVanishedTicketsPlace() {
        StatusColumns columns = columnsOf(ticketViewNumbered(1, Status.TODO), ticketViewNumbered(2, Status.TODO));

        assertThat(columns.replacing(new StatusColumnsPlace(1, 1)).map(ticket -> ticket.id().value())).contains("2");
        assertThat(columns.replacing(new StatusColumnsPlace(1, 5)).map(ticket -> ticket.id().value()))
                .as("the column got shorter than the row")
                .contains("2");
    }

    @Test
    void shouldLandInTheNearestColumnWhenTheVanishedTicketsColumnIsNowEmpty() {
        StatusColumns columns = columnsOf(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(4, Status.IN_REVIEW));

        assertThat(columns.replacing(new StatusColumnsPlace(1, 0)).map(ticket -> ticket.id().value()))
                .as("todo is empty; the next column on the right is tried before the left")
                .contains("4");
        assertThat(columns.replacing(new StatusColumnsPlace(3, 0)).map(ticket -> ticket.id().value())).contains("4");
    }

    @Test
    void shouldLandOnNothingWhenNoColumnHoldsATicket() {
        assertThat(columnsOf(ticketViewNumbered(9, Status.DONE)).replacing(new StatusColumnsPlace(0, 0))).isEmpty();
    }

    @Test
    void shouldLocateATicketByItsColumnAndRow() {
        StatusColumns columns = columnsOf(ticketViewNumbered(1, Status.TODO), ticketViewNumbered(2, Status.TODO));

        assertThat(columns.locate(ticketIdNumbered(2))).contains(new StatusColumnsPlace(1, 1));
    }
}

package dev.maxenceng.lazyclaude.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class BoardTest {

    private static Ticket ticket(String id, Status status) {
        return Ticket.builder().id(TicketId.of(id)).status(status).build();
    }

    @Test
    void shouldGroupTicketsByStatus() {
        Board board = Board.of(
                List.of(ticket("A-1", Status.TODO), ticket("A-2", Status.DONE), ticket("A-3", Status.TODO)), List.of());

        assertThat(board.tickets(Status.TODO)).extracting(t -> t.id().value()).containsExactly("A-1", "A-3");
        assertThat(board.tickets(Status.DONE)).extracting(t -> t.id().value()).containsExactly("A-2");
    }

    @Test
    void shouldOrderTicketsWithinAGroupNumerically() {
        Board board = Board.of(
                List.of(ticket("PROJ-10", Status.DRAFT), ticket("PROJ-2", Status.DRAFT), ticket("PROJ-1", Status.DRAFT)),
                List.of());

        assertThat(board.tickets(Status.DRAFT))
                .extracting(t -> t.id().value())
                .containsExactly("PROJ-1", "PROJ-2", "PROJ-10");
    }

    @Test
    void shouldKeepAnEmptyGroupForAStatusWithNoTickets() {
        Board board = Board.of(List.of(ticket("A-1", Status.DONE)), List.of());

        assertThat(board.tickets(Status.TODO)).isEmpty();
    }

    @Test
    void shouldOrderUnreadableTicketsByFile() {
        Board board = Board.of(
                List.of(),
                List.of(new UnreadableTicket("b.md", "broken"), new UnreadableTicket("a.md", "broken too")));

        assertThat(board.unreadable()).extracting(UnreadableTicket::file).containsExactly("a.md", "b.md");
    }

    @Test
    void shouldNotBeAbleToChangeTheTicketsItWasGiven() {
        Board board = Board.of(List.of(ticket("A-1", Status.TODO)), List.of());

        assertThatThrownBy(() -> board.tickets(Status.TODO).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

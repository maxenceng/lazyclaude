package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Status;

public final class BoardViewFixture {

    private BoardViewFixture() {}

    public static BoardView boardViewOf(TicketView... tickets) {
        return boardViewOf(List.of(tickets), List.of());
    }

    public static BoardView boardViewOf(List<TicketView> tickets, List<UnreadableTicketView> unreadable) {
        return BoardView.builder()
                .groups(Arrays.stream(Status.values())
                        .map(status -> StatusGroupView.builder()
                                .status(status)
                                .tickets(tickets.stream()
                                        .filter(ticket -> ticket.status() == status)
                                        .toList())
                                .build())
                        .toList())
                .unreadable(unreadable)
                .build();
    }
}

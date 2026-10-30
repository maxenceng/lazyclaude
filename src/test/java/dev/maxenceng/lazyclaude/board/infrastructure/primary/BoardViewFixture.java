package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.ProjectRoot;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.ProjectFixture;

public final class BoardViewFixture {

    private BoardViewFixture() {}

    public static ProjectRoot projectRoot() {
        return new ProjectRoot(ProjectFixture.project().root());
    }

    public static BoardView unavailableBoardView() {
        return BoardView.builder()
                .groups(Arrays.stream(Status.values())
                        .map(status -> StatusGroupView.builder().status(status).tickets(List.of()).build())
                        .toList())
                .unreadable(List.of())
                .root(projectRoot())
                .unavailable(Optional.of(new Reason("gh is not logged in; run gh auth login")))
                .build();
    }

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
                .root(projectRoot())
                .build();
    }
}

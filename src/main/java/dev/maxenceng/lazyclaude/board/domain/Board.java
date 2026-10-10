package dev.maxenceng.lazyclaude.board.domain;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A project's tickets grouped by status, plus the backlog files that could not be read. */
public final class Board {

    private final Map<Status, List<Ticket>> byStatus = new EnumMap<>(Status.class);
    private final List<UnreadableTicket> unreadable;

    private Board(List<Ticket> tickets, List<UnreadableTicket> unreadable) {
        for (Status status : Status.values()) {
            byStatus.put(
                    status,
                    tickets.stream()
                            .filter(ticket -> ticket.status() == status)
                            .sorted(Comparator.comparing(Ticket::id))
                            .toList());
        }
        this.unreadable = unreadable.stream()
                .sorted(Comparator.comparing(UnreadableTicket::file))
                .toList();
    }

    public static Board of(List<Ticket> tickets, List<UnreadableTicket> unreadable) {
        return new Board(List.copyOf(tickets), List.copyOf(unreadable));
    }

    public List<Ticket> tickets(Status status) {
        return byStatus.get(status);
    }

    public List<UnreadableTicket> unreadable() {
        return unreadable;
    }
}

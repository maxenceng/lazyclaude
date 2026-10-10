package dev.maxenceng.lazyclaude.board.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Builds the board of the project a working directory belongs to. It owns the board's rules:
 * one group per status in lifecycle order, tickets in id order, and one ticket per id — a later
 * file claiming an id already read becomes an unreadable ticket, whoever produced the entries.
 */
public record BoardManager(ProjectLocatorPort locator, BacklogPort backlog) {

    public Board boardFor(WorkingDirectory from) {
        Project project = locator.locate(from);
        return board(backlog.entries(project));
    }

    private static Board board(List<BacklogEntry> entries) {
        List<Ticket> tickets = new ArrayList<>();
        List<UnreadableTicket> unreadable = new ArrayList<>();
        Set<TicketId> claimed = new HashSet<>();
        for (BacklogEntry entry : entries) {
            switch (entry) {
                case UnreadableTicket refused -> unreadable.add(refused);
                case TicketFile read when !claimed.add(read.ticket().id()) -> unreadable.add(alreadyClaimed(read));
                case TicketFile read -> tickets.add(read.ticket());
            }
        }
        unreadable.sort(Comparator.comparing(UnreadableTicket::file));
        return new Board(groups(tickets), unreadable);
    }

    private static UnreadableTicket alreadyClaimed(TicketFile read) {
        return new UnreadableTicket(read.file(), "id %s is already used".formatted(read.ticket().id().value()));
    }

    private static List<StatusGroup> groups(List<Ticket> tickets) {
        return Arrays.stream(Status.values())
                .map(status -> new StatusGroup(
                        status,
                        tickets.stream()
                                .filter(ticket -> ticket.status() == status)
                                .sorted(Comparator.comparing(Ticket::id))
                                .toList()))
                .toList();
    }
}

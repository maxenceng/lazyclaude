package dev.maxenceng.lazyclaude.board.domain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Builds the board of the project a working directory belongs to. It owns the board's rules:
 * one group per status in lifecycle order, tickets in id order, and one ticket per id — a later
 * file claiming an id already read becomes an unreadable ticket, whoever produced the entries.
 */
public record BoardManager(ProjectLocatorPort locator, BacklogPort backlog) {

    public Board boardFor(WorkingDirectory from) {
        Project project = locator.locate(from);
        return board(project, backlog.entries(project));
    }

    private static Board board(Project project, List<BacklogEntry> entries) {
        Map<TicketId, List<TicketFile>> byId = entries.stream()
                .flatMap(entry -> entry instanceof TicketFile read ? Stream.of(read) : Stream.empty())
                .collect(Collectors.groupingBy(read -> read.ticket().id(), LinkedHashMap::new, Collectors.toList()));
        Stream<UnreadableTicket> refused = entries.stream()
                .flatMap(entry -> entry instanceof UnreadableTicket unreadable ? Stream.of(unreadable) : Stream.empty());
        Stream<UnreadableTicket> claimedTwice = byId.values().stream()
                .flatMap(files -> files.stream().skip(1))
                .map(BoardManager::alreadyClaimed);
        return Board.builder()
                .project(project)
                .groups(groups(byId.values().stream()
                        .map(files -> files.getFirst().ticket())
                        .toList()))
                .unreadable(Stream.concat(refused, claimedTwice)
                        .sorted(Comparator.comparing(UnreadableTicket::file))
                        .toList())
                .build();
    }

    private static UnreadableTicket alreadyClaimed(TicketFile read) {
        return UnreadableTicket.builder()
                .file(read.file())
                .reason(new Reason("id %s is already used".formatted(read.ticket().id().value())))
                .build();
    }

    private static List<StatusGroup> groups(List<Ticket> tickets) {
        return Arrays.stream(Status.values())
                .map(status -> StatusGroup.builder()
                        .status(status)
                        .tickets(tickets.stream()
                                .filter(ticket -> ticket.status() == status)
                                .sorted(Comparator.comparing(Ticket::id))
                                .toList())
                        .build())
                .toList();
    }
}

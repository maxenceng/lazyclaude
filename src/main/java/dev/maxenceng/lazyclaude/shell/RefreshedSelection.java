package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.StatusGroupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;

/**
 * Where the Board's selection goes when its tickets are read again and the selected one is no longer a visible row, because it is gone
 * or moved into a folded group: onto the next ticket of the group it left, else the previous one, else that group's header.
 */
final class RefreshedSelection {

    private RefreshedSelection() {}

    /** Nothing when the selection is not a ticket, or is still visible; those keep the panel's own rule. */
    static Optional<RowKey> of(RowKey was, BoardView before, List<RowKey> visible) {
        if (was.kind() != RowKind.TICKET || visible.contains(was)) {
            return Optional.empty();
        }
        return groupHolding(before, was).flatMap(group -> {
            List<TicketView> tickets = group.tickets();
            int at = IntStream.range(0, tickets.size())
                    .filter(index -> keyOf(tickets.get(index)).equals(was))
                    .findFirst()
                    .orElseThrow();
            return Stream.concat(
                            tickets.subList(at + 1, tickets.size()).stream(),
                            tickets.subList(0, at).reversed().stream())
                    .map(RefreshedSelection::keyOf)
                    .filter(visible::contains)
                    .findFirst()
                    .or(() -> Optional.of(headerOf(group)).filter(visible::contains));
        });
    }

    private static Optional<StatusGroupView> groupHolding(BoardView board, RowKey ticket) {
        return board.groups().stream()
                .filter(group -> group.tickets().stream().anyMatch(held -> keyOf(held).equals(ticket)))
                .findFirst();
    }

    private static RowKey keyOf(TicketView ticket) {
        return new TicketRow(ticket).key();
    }

    private static RowKey headerOf(StatusGroupView group) {
        return new RowKey(RowKind.GROUP, group.status().label());
    }
}

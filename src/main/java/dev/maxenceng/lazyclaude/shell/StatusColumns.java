package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.StatusGroupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.TicketId;

/**
 * The Board as status columns, the zoom's view of it: where a cursor can stand and where it moves. A finished status is a narrow column
 * the cursor skips, so the cursor stands only on the tickets of the columns that share the width.
 */
record StatusColumns(BoardReadingView board) {

    private static final int NEXT = 1;
    private static final int PREVIOUS = -1;

    /** The columns that share the width, in lifecycle order. */
    List<StatusGroupView> shared() {
        return BoardRows.groupsOf(board).stream().filter(group -> !group.status().finished()).toList();
    }

    /** The narrow columns, a header and a count each, in lifecycle order. */
    List<StatusGroupView> narrow() {
        return BoardRows.groupsOf(board).stream().filter(group -> group.status().finished()).toList();
    }

    Optional<TicketView> first() {
        return shared().stream().flatMap(group -> group.tickets().stream()).findFirst();
    }

    Optional<StatusColumnsPlace> locate(TicketId ticketId) {
        List<StatusGroupView> columns = shared();
        return IntStream.range(0, columns.size())
                .boxed()
                .flatMap(column -> IntStream.range(0, columns.get(column).tickets().size())
                        .filter(row -> columns.get(column).tickets().get(row).id().equals(ticketId))
                        .mapToObj(row -> new StatusColumnsPlace(column, row)))
                .findFirst();
    }

    Optional<TicketView> ticket(TicketId ticketId) {
        return locate(ticketId).flatMap(this::ticketAt);
    }

    Optional<TicketView> ticketAt(StatusColumnsPlace at) {
        List<TicketView> tickets = shared().get(at.column()).tickets();
        return tickets.isEmpty() ? Optional.empty() : Optional.of(tickets.get(Math.min(at.row(), tickets.size() - 1)));
    }

    Optional<TicketView> down(TicketId ticketId) {
        return moved(ticketId, at -> new StatusColumnsPlace(at.column(), at.row() + NEXT));
    }

    Optional<TicketView> up(TicketId ticketId) {
        return moved(ticketId, at -> new StatusColumnsPlace(at.column(), Math.max(0, at.row() + PREVIOUS)));
    }

    /** A wheel notch over {@code column}: a ticket along it where the cursor stands in it, else into it at the cursor's row; an empty column keeps the cursor. */
    Optional<TicketView> wheeled(TicketId ticketId, int column, boolean down) {
        Optional<StatusColumnsPlace> at = locate(ticketId);
        if (at.filter(place -> place.column() == column).isPresent()) {
            return down ? down(ticketId) : up(ticketId);
        }
        return ticketAt(new StatusColumnsPlace(column, at.map(StatusColumnsPlace::row).orElse(0))).or(() -> ticket(ticketId));
    }

    /** The next column that holds a ticket, at the same row or the last one it has. */
    Optional<TicketView> right(TicketId ticketId) {
        return sideways(ticketId, NEXT);
    }

    Optional<TicketView> left(TicketId ticketId) {
        return sideways(ticketId, PREVIOUS);
    }

    /** Where the cursor lands when the ticket it stood at {@code was} is gone: the row that took its place, else the nearest column with a ticket. */
    Optional<TicketView> replacing(StatusColumnsPlace was) {
        return nonEmptyFrom(was.column(), 0, NEXT)
                .or(() -> nonEmptyFrom(was.column(), 0, PREVIOUS))
                .flatMap(column -> ticketAt(new StatusColumnsPlace(column, was.row())));
    }

    private Optional<TicketView> moved(TicketId ticketId, UnaryOperator<StatusColumnsPlace> step) {
        return locate(ticketId).flatMap(at -> ticketAt(step.apply(at))).or(this::first);
    }

    private Optional<TicketView> sideways(TicketId ticketId, int direction) {
        return locate(ticketId)
                .map(at -> nonEmptyFrom(at.column(), direction, direction)
                        .flatMap(column -> ticketAt(new StatusColumnsPlace(column, at.row())))
                        .or(() -> ticketAt(at)))
                .orElseGet(this::first);
    }

    private Optional<Integer> nonEmptyFrom(int column, int offset, int direction) {
        List<StatusGroupView> columns = shared();
        IntUnaryOperator next = index -> index + direction;
        return IntStream.iterate(column + offset, index -> index >= 0 && index < columns.size(), next)
                .filter(index -> !columns.get(index).tickets().isEmpty())
                .boxed()
                .findFirst();
    }
}

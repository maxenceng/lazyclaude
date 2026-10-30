package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.StatusGroupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnavailableBoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketView;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** The Board panel's rows and the two things the shell remembers about them: the selection and the folded groups. */
final class BoardRows {

    static final String UNREADABLE_GROUP = "unreadable";
    private static final int DOWN = 1;
    private static final int UP = -1;

    private BoardReadingView board;
    private Set<RowKey> folded;
    private RowKey selected;

    BoardRows(BoardReadingView board) {
        this.board = board;
        this.folded = groupsOf(board).stream()
                .filter(group -> group.status().finished())
                .map(group -> groupKey(group.status().label()))
                .collect(Collectors.toUnmodifiableSet());
        this.selected = visible().getFirst().key();
    }

    /** The ids of the tickets on the board, which {@code session} links its sessions against. */
    static List<LinkedTicketId> ticketIdsOf(BoardReadingView board) {
        return groupsOf(board).stream()
                .flatMap(group -> group.tickets().stream())
                .map(ticket -> new LinkedTicketId(ticket.id().value()))
                .toList();
    }

    /** The ticket on {@code board} with {@code id}, if it has one. */
    static Optional<TicketView> ticketOf(BoardReadingView board, LinkedTicketId id) {
        return groupsOf(board).stream()
                .flatMap(group -> group.tickets().stream())
                .filter(ticket -> ticket.id().value().equals(id.value()))
                .findFirst();
    }

    /** Swaps in a freshly read board; the selection stays on its ticket id, following it into its new status group (see {@link RefreshedSelection}). */
    void replace(BoardReadingView next) {
        RowKey was = selected;
        int index = selectedIndex();
        BoardReadingView before = board;
        board = next;
        List<RowKey> keys = visible().stream().map(Row::key).toList();
        selected = RefreshedSelection.of(was, before, keys)
                .or(() -> RowSelection.kept(Optional.of(was), index, keys))
                .orElseGet(keys::getFirst);
    }

    /** Swaps in a freshly read board while the status columns are open: the cursor follows its ticket, else the row that took its place. */
    void replaceInStatusColumns(BoardReadingView next) {
        Optional<TicketId> was = selectedTicketId();
        Optional<StatusColumnsPlace> at = was.flatMap(statusColumns()::locate);
        replace(next);
        StatusColumns now = statusColumns();
        was.flatMap(now::ticket).or(() -> at.flatMap(now::replacing)).or(now::first).ifPresent(this::moveTo);
    }

    StatusColumns statusColumns() {
        return new StatusColumns(board);
    }

    /** Moves the selection, which is the cursor of the status columns, by {@code move}; onto the first ticket when it stands on none. */
    void moveInStatusColumns(BiFunction<StatusColumns, TicketId, Optional<TicketView>> move) {
        StatusColumns columns = statusColumns();
        selectedTicketId().flatMap(id -> move.apply(columns, id)).or(columns::first).ifPresent(this::moveTo);
    }

    /** The ticket on the board as last read with {@code id}, if it has one. */
    Optional<TicketView> ticketNumbered(TicketId id) {
        return groupsOf(board).stream()
                .flatMap(group -> group.tickets().stream())
                .filter(ticket -> ticket.id().equals(id))
                .findFirst();
    }

    /** Puts the selection on {@code ticket}, unfolding its group. */
    void selectTicket(TicketView ticket) {
        moveTo(ticket);
    }

    /** Puts the selection on a ticket the status columns can stand on, if it is not on one. */
    void enterStatusColumns() {
        StatusColumns columns = statusColumns();
        selectedTicketId().flatMap(columns::ticket).or(columns::first).ifPresent(this::moveTo);
    }

    private Optional<TicketId> selectedTicketId() {
        return selected() instanceof TicketRow(TicketView ticket) ? Optional.of(ticket.id()) : Optional.empty();
    }

    private void moveTo(TicketView ticket) {
        folded = FoldedKeys.unfolded(folded, groupKey(ticket.status().label()));
        selected = new TicketRow(ticket).key();
    }

    /** The status groups of a board read; one whose issues could not be read has none. */
    static List<StatusGroupView> groupsOf(BoardReadingView board) {
        return switch (board) {
            case BoardView read -> read.groups();
            case UnavailableBoardView _ -> List.of();
        };
    }

    List<Row> visible() {
        return switch (board) {
            case BoardView read -> Stream.concat(read.groups().stream().flatMap(this::groupRows), unreadableRows(read.unreadable()))
                    .toList();
            case UnavailableBoardView unavailable -> List.of(new UnavailableRow(unavailable.reason()));
        };
    }

    private Stream<Row> unreadableRows(List<UnreadableTicketView> unreadable) {
        if (unreadable.isEmpty()) {
            return Stream.empty();
        }
        boolean isFolded = folded.contains(groupKey(UNREADABLE_GROUP));
        Stream<Row> header = Stream.of(new HeaderRow(UNREADABLE_GROUP, unreadable.size(), isFolded));
        return isFolded ? header : Stream.concat(header, unreadable.stream().map(UnreadableRow::new));
    }

    Row selected() {
        List<Row> rows = visible();
        return rows.get(indexOfSelected(rows));
    }

    int selectedIndex() {
        return indexOfSelected(visible());
    }

    Optional<HeaderRow> selectedHeader() {
        return selected() instanceof HeaderRow header ? Optional.of(header) : Optional.empty();
    }

    /** Puts the selection on the row at {@code index} of the visible ones, when there is one. */
    void selectAt(int index) {
        visible().stream().skip(index).findFirst().map(Row::key).ifPresent(key -> selected = key);
    }

    void down() {
        move(DOWN);
    }

    void up() {
        move(UP);
    }

    void toggleFold() {
        selectedHeader().map(HeaderRow::key).ifPresent(key -> folded = FoldedKeys.toggled(folded, key));
    }

    private void move(int step) {
        List<Row> rows = visible();
        int target = Math.clamp(indexOfSelected(rows) + (long) step, 0, rows.size() - 1);
        selected = rows.get(target).key();
    }

    private Stream<Row> groupRows(StatusGroupView group) {
        boolean isFolded = folded.contains(groupKey(group.status().label()));
        Stream<Row> header = Stream.of(new HeaderRow(group.status().label(), group.tickets().size(), isFolded));
        return isFolded ? header : Stream.concat(header, group.tickets().stream().map(TicketRow::new));
    }

    private int indexOfSelected(List<Row> rows) {
        return IntStream.range(0, rows.size())
                .filter(index -> rows.get(index).key().equals(selected))
                .findFirst()
                .orElseThrow(() -> new SelectedRowNotVisibleException(selected));
    }

    private static RowKey groupKey(String group) {
        return new RowKey(RowKind.GROUP, group);
    }
}

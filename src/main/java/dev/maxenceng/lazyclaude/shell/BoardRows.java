package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.StatusGroupView;

/** The Board panel's rows and the two things the shell remembers about them: the selection and the folded groups. */
final class BoardRows {

    private static final String UNREADABLE_GROUP = "unreadable";
    private static final int DOWN = 1;
    private static final int UP = -1;

    private BoardView board;
    private final Set<RowKey> folded = new HashSet<>();
    private RowKey selected;

    BoardRows(BoardView board) {
        this.board = board;
        board.groups().stream()
                .filter(group -> group.status().finished())
                .forEach(group -> folded.add(groupKey(group.status().label())));
        this.selected = visible().getFirst().key();
    }

    /** The branches of the tickets on the board, which is how a merged branch is told from one that belongs to no ticket. */
    static Set<String> ticketBranchesOf(BoardView board) {
        return board.groups().stream()
                .flatMap(group -> group.tickets().stream())
                .map(ticket -> ticket.branch().value())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Swaps in a freshly read board; the selection stays on its ticket id, or on the row that took its place. */
    void replace(BoardView next) {
        RowKey was = selected;
        int index = selectedIndex();
        board = next;
        List<RowKey> keys = visible().stream().map(Row::key).toList();
        selected = RowSelection.kept(Optional.of(was), index, keys).orElseGet(() -> keys.getFirst());
    }

    List<Row> visible() {
        List<Row> rows = new ArrayList<>(
                board.groups().stream().flatMap(this::groupRows).toList());
        if (!board.unreadable().isEmpty()) {
            boolean isFolded = folded.contains(groupKey(UNREADABLE_GROUP));
            rows.add(new HeaderRow(UNREADABLE_GROUP, board.unreadable().size(), isFolded));
            if (!isFolded) {
                board.unreadable().forEach(unreadable -> rows.add(new UnreadableRow(unreadable)));
            }
        }
        return rows;
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

    void down() {
        move(DOWN);
    }

    void up() {
        move(UP);
    }

    void toggleFold() {
        selectedHeader().map(HeaderRow::key).ifPresent(key -> {
            if (!folded.remove(key)) {
                folded.add(key);
            }
        });
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

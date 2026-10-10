package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.StatusGroupView;

/** The Board panel's rows and the two things the shell remembers about them: the selection and the folded groups. */
final class BoardRows {

    private static final String UNREADABLE_GROUP = "unreadable";
    private static final int DOWN = 1;
    private static final int UP = -1;

    private final BoardView board;
    private final Set<RowKey> folded = new HashSet<>();
    private RowKey selected;

    BoardRows(BoardView board) {
        this.board = board;
        board.groups().stream()
                .filter(group -> group.status().finished())
                .forEach(group -> folded.add(groupKey(group.status().label())));
        this.selected = visible().getFirst().key();
    }

    List<Row> visible() {
        List<Row> rows = new ArrayList<>();
        for (StatusGroupView group : board.groups()) {
            boolean isFolded = folded.contains(groupKey(group.status().label()));
            rows.add(new HeaderRow(group.status().label(), group.tickets().size(), isFolded));
            if (!isFolded) {
                group.tickets().forEach(ticket -> rows.add(new TicketRow(ticket)));
            }
        }
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

    private int indexOfSelected(List<Row> rows) {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).key().equals(selected)) {
                return index;
            }
        }
        throw new SelectedRowNotVisibleException(selected);
    }

    private static RowKey groupKey(String group) {
        return new RowKey(RowKind.GROUP, group);
    }
}

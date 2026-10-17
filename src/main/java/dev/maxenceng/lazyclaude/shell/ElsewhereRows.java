package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.AvailableSessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ElsewhereGroupView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;

/**
 * The Elsewhere panel's rows and the selection, by session id so it survives the sessions being read again: one header per
 * place, always unfolded, then its sessions in the order the context gave. The cursor skips the headers.
 */
final class ElsewhereRows {

    private static final int DOWN = 1;
    private static final int UP = -1;

    private SessionsView sessions;
    private Optional<RowKey> selected;

    ElsewhereRows(SessionsView sessions) {
        this.sessions = sessions;
        this.selected = firstSelectable();
    }

    List<ElsewhereRow> visible() {
        return groups().stream()
                .flatMap(group -> Stream.concat(
                        Stream.<ElsewhereRow>of(new ElsewhereHeaderRow(group.name(), group.sessions().size())),
                        group.sessions().stream().map(session -> new ElsewhereSessionRow(group.name(), session))))
                .toList();
    }

    /** The places outside the project, strongest first; none while the sessions could not be read. */
    List<ElsewhereGroupView> groups() {
        return sessions instanceof AvailableSessionsView available ? available.elsewhere() : List.of();
    }

    boolean isEmpty() {
        return selected.isEmpty();
    }

    Optional<ElsewhereSessionRow> selected() {
        return selected.flatMap(key -> visible().stream()
                .filter(ElsewhereSessionRow.class::isInstance)
                .map(ElsewhereSessionRow.class::cast)
                .filter(row -> row.key().equals(key))
                .findFirst());
    }

    int selectedIndex() {
        List<ElsewhereRow> rows = visible();
        return selected
                .flatMap(key -> IntStream.range(0, rows.size())
                        .filter(index -> rows.get(index).key().equals(key))
                        .boxed()
                        .findFirst())
                .orElse(0);
    }

    Optional<RowKey> selectedKey() {
        return selected;
    }

    void down() {
        move(DOWN);
    }

    void up() {
        move(UP);
    }

    /** Swaps in freshly read sessions; the selection stays on its id, or on the row that took its place. Says whether it moved. */
    boolean replace(SessionsView next) {
        Optional<RowKey> was = selected;
        int index = selectableIndexOfSelected();
        sessions = next;
        selected = RowSelection.kept(was, index, selectableKeys());
        return !was.equals(selected);
    }

    private void move(int step) {
        List<RowKey> keys = selectableKeys();
        if (keys.isEmpty()) {
            return;
        }
        int target = Math.clamp(selectableIndexOfSelected() + (long) step, 0, keys.size() - 1);
        selected = Optional.of(keys.get(target));
    }

    private Optional<RowKey> firstSelectable() {
        return selectableKeys().stream().findFirst();
    }

    private List<RowKey> selectableKeys() {
        return visible().stream().filter(ElsewhereRow::selectable).map(ElsewhereRow::key).toList();
    }

    private int selectableIndexOfSelected() {
        List<RowKey> keys = selectableKeys();
        return selected.map(keys::indexOf).filter(index -> index >= 0).orElse(0);
    }
}

package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.FolderView;

/**
 * The Docs panel's rows and what the shell remembers about them: the selection and the folded
 * folders, both by path, so they survive the tree being read again.
 */
final class DocsRows {

    private static final String PATH_SEPARATOR = "/";
    private static final int DOWN = 1;
    private static final int UP = -1;

    private final Set<RowKey> folded = new HashSet<>();
    private DocsView docs;
    private Optional<RowKey> selected;

    DocsRows(DocsView docs) {
        this.docs = docs;
        this.selected = visible().stream().findFirst().map(DocsRow::key);
    }

    List<DocsRow> visible() {
        List<DocsRow> rows = new ArrayList<>();
        list(rows, docs.root(), "", 0);
        return rows;
    }

    boolean isEmpty() {
        return selected.isEmpty();
    }

    Optional<DocsRow> selected() {
        List<DocsRow> rows = visible();
        return selected.map(key -> rows.get(indexOf(rows, key)));
    }

    int selectedIndex() {
        List<DocsRow> rows = visible();
        return selected.map(key -> indexOf(rows, key)).orElse(0);
    }

    Optional<FolderRow> selectedFolder() {
        return selected().filter(FolderRow.class::isInstance).map(FolderRow.class::cast);
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

    void toggleFold() {
        selectedFolder().map(FolderRow::key).ifPresent(key -> {
            if (!folded.remove(key)) {
                folded.add(key);
            }
        });
    }

    /** Swaps in a freshly read tree; the selection stays on its path, or on the row that took its place. Says whether the selection moved. */
    boolean replace(DocsView next) {
        Optional<RowKey> was = selected;
        int index = selectedIndex();
        docs = next;
        List<DocsRow> after = visible();
        if (after.isEmpty()) {
            selected = Optional.empty();
        } else if (!was.map(key -> contains(after, key)).orElse(false)) {
            selected = Optional.of(after.get(Math.min(index, after.size() - 1)).key());
        }
        return !was.equals(selected);
    }

    private void move(int step) {
        List<DocsRow> rows = visible();
        if (rows.isEmpty()) {
            return;
        }
        int target = Math.clamp(selectedIndex() + (long) step, 0, rows.size() - 1);
        selected = Optional.of(rows.get(target).key());
    }

    private void list(List<DocsRow> rows, FolderView folder, String parent, int depth) {
        folder.documents().forEach(document -> rows.add(new DocumentRow(document, depth)));
        for (FolderView child : folder.folders()) {
            String path = parent + child.name().value();
            boolean isFolded = folded.contains(new RowKey(RowKind.FOLDER, path));
            rows.add(new FolderRow(path, child.name().value(), child.count(), isFolded, depth));
            if (!isFolded) {
                list(rows, child, path + PATH_SEPARATOR, depth + 1);
            }
        }
    }

    private static boolean contains(List<DocsRow> rows, RowKey key) {
        return rows.stream().anyMatch(row -> row.key().equals(key));
    }

    private static int indexOf(List<DocsRow> rows, RowKey key) {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).key().equals(key)) {
                return index;
            }
        }
        throw new SelectedRowNotVisibleException(key);
    }
}

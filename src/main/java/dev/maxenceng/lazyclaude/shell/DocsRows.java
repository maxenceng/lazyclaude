package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsTreeView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocumentView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.FolderView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.UnavailableDocsView;
import dev.maxenceng.lazyclaude.docs.published.Reason;

/**
 * The Docs panel's rows and what the shell remembers about them: the selection and the folded
 * folders, both by path, so they survive the tree being read again.
 */
final class DocsRows {

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
        switch (docs) {
            case DocsTreeView tree -> list(rows, tree.documents(), tree.folders(), 0);
            case UnavailableDocsView unavailable -> {}
        }
        return rows;
    }

    /** Why there is no tree to show, when {@code docs/} could not be listed. */
    Optional<Reason> unavailable() {
        return docs instanceof UnavailableDocsView unavailable ? Optional.of(unavailable.reason()) : Optional.empty();
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
        selected = RowSelection.kept(was, index, visible().stream().map(DocsRow::key).toList());
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

    private void list(List<DocsRow> rows, List<DocumentView> documents, List<FolderView> folders, int depth) {
        documents.forEach(document -> rows.add(new DocumentRow(document, depth)));
        for (FolderView folder : folders) {
            String path = folder.path().value();
            boolean isFolded = folded.contains(new RowKey(RowKind.FOLDER, path));
            rows.add(new FolderRow(path, folder.name().value(), folder.count(), isFolded, depth));
            if (!isFolded) {
                list(rows, folder.documents(), folder.folders(), depth + 1);
            }
        }
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

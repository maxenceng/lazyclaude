package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    private Set<RowKey> folded = Set.of();
    private DocsView docs;
    private Optional<RowKey> selected;

    DocsRows(DocsView docs) {
        this.docs = docs;
        this.selected = visible().stream().findFirst().map(DocsRow::key);
    }

    List<DocsRow> visible() {
        return switch (docs) {
            case DocsTreeView tree -> list(tree.documents(), tree.folders(), 0).toList();
            case UnavailableDocsView _ -> List.of();
        };
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
        selectedFolder().map(FolderRow::key).ifPresent(key -> folded = FoldedKeys.toggled(folded, key));
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

    private Stream<DocsRow> list(List<DocumentView> documents, List<FolderView> folders, int depth) {
        return Stream.concat(
                documents.stream().map(document -> new DocumentRow(document, depth)),
                folders.stream().flatMap(folder -> folderRows(folder, depth)));
    }

    private Stream<DocsRow> folderRows(FolderView folder, int depth) {
        String path = folder.path().value();
        boolean isFolded = folded.contains(new RowKey(RowKind.FOLDER, path));
        Stream<DocsRow> header = Stream.of(new FolderRow(path, folder.name().value(), folder.count(), isFolded, depth));
        return isFolded ? header : Stream.concat(header, list(folder.documents(), folder.folders(), depth + 1));
    }

    private static int indexOf(List<DocsRow> rows, RowKey key) {
        return IntStream.range(0, rows.size())
                .filter(index -> rows.get(index).key().equals(key))
                .findFirst()
                .orElseThrow(() -> new SelectedRowNotVisibleException(key));
    }
}

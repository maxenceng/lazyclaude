package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;

/** Where a panel's selection goes when its rows are rebuilt. */
final class RowSelection {

    private RowSelection() {}

    /** The selection stays on its row if that is still there, else lands on the row that took its place, else on the last one. */
    static Optional<RowKey> kept(Optional<RowKey> was, int index, List<RowKey> after) {
        if (after.isEmpty()) {
            return Optional.empty();
        }
        if (was.filter(after::contains).isPresent()) {
            return was;
        }
        return Optional.of(after.get(Math.min(index, after.size() - 1)));
    }

    /** The selection {@code step} rows on among the selectable {@code keys}, held at the first or last; unchanged when there are none. */
    static Optional<RowKey> moved(Optional<RowKey> selected, int step, List<RowKey> keys) {
        if (keys.isEmpty()) {
            return selected;
        }
        return Optional.of(keys.get((int) Math.clamp(indexIn(selected, keys) + (long) step, 0, keys.size() - 1)));
    }

    /** Where the selection stands among the selectable {@code keys}, the first when it is on none of them. */
    static int indexIn(Optional<RowKey> selected, List<RowKey> keys) {
        return selected.map(keys::indexOf).filter(index -> index >= 0).orElse(0);
    }
}

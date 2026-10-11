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
}

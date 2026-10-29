package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What the last frame drew that the mouse reaches, kept by the render thread: a hit added later was drawn over an earlier one,
 * so it wins where they overlap. The frame empties it before drawing, so a pane that is not drawn is not reached.
 */
final class HitMap {

    private List<Hit> hits = List.of();
    private Optional<RowsDrawn> picker = Optional.empty();

    void clear() {
        hits = List.of();
        picker = Optional.empty();
    }

    void add(Hit hit) {
        hits = Stream.concat(hits.stream(), Stream.of(hit)).toList();
    }

    void addAll(List<? extends Hit> added) {
        hits = Stream.concat(hits.stream(), added.stream()).toList();
    }

    void pickerDrawn(RowsDrawn rows) {
        picker = Optional.of(rows);
    }

    Optional<Hit> at(int x, int y) {
        return hits.reversed().stream().filter(hit -> hit.area().contains(x, y)).findFirst();
    }

    /** The index of the picker's row at the point, none when the point is not on one. */
    Optional<Integer> pickerRowAt(int x, int y) {
        return picker.filter(rows -> rows.area().contains(x, y)).flatMap(rows -> rows.rowAt(y));
    }
}

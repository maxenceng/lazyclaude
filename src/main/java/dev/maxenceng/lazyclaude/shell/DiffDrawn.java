package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.tamboui.layout.Rect;

/** Where the diff drew its lines: the cells it was given and, for each screen row from the top, the diff line drawn there if it is one. */
record DiffDrawn(Rect area, List<Optional<Integer>> lines) {

    /** The diff line drawn on line {@code y}, none on a header, a thread, a comment or below the last row drawn. */
    Optional<Integer> lineAt(int y) {
        int drawn = y - area.y();
        return drawn >= 0 && drawn < lines.size() ? lines.get(drawn) : Optional.empty();
    }

    /** The diff line nearest line {@code y}, the nearest drawn line above or below the diff when {@code y} is outside it; none when no line is drawn. */
    Optional<Integer> nearestLineAt(int y) {
        int drawn = Math.clamp(y - area.y(), 0, Math.max(0, lines.size() - 1));
        return IntStream.rangeClosed(0, lines.size())
                .boxed()
                .flatMap(distance -> IntStream.of(drawn - distance, drawn + distance).boxed())
                .filter(row -> row >= 0 && row < lines.size())
                .flatMap(row -> lines.get(row).stream())
                .findFirst();
    }
}

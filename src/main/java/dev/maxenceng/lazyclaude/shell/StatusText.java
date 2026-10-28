package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.tamboui.style.Style;

/** What the status line's left side says: how many of the project's live sessions ask each thing of you. */
final class StatusText {

    private static final String COUNT = "%s %d";
    private static final StatusSpan GAP = new StatusSpan("  ", Style.EMPTY);
    private static final List<Marker> STRONGEST_FIRST = List.of(Marker.values()).reversed();

    private StatusText() {}

    /** {@code counts} says how many live sessions of the project carry each marker. */
    static List<StatusSpan> spans(Map<Marker, Long> counts) {
        List<List<StatusSpan>> own = STRONGEST_FIRST.stream()
                .filter(counts::containsKey)
                .map(marker -> List.of(new StatusSpan(
                        COUNT.formatted(SessionText.glyph(marker), counts.get(marker)),
                        Style.EMPTY.fg(SessionText.colour(marker)))))
                .toList();
        return IntStream.range(0, own.size())
                .boxed()
                .flatMap(index -> Stream.concat(index == 0 ? Stream.empty() : Stream.of(GAP), own.get(index).stream()))
                .toList();
    }
}

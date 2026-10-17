package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.ElsewhereGroupView;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.tamboui.style.Style;

/**
 * What the status line's left side says: how many of the project's live sessions ask each thing of you, then the other
 * projects with a session waiting on you, each under its strongest waiting marker.
 */
final class StatusText {

    private static final String COUNT = "%s %d";
    private static final String PLACE = " %s";
    private static final StatusSpan GAP = new StatusSpan("  ", Style.EMPTY);
    private static final StatusSpan SECTION_GAP = new StatusSpan("   ", Style.EMPTY);
    private static final StatusSpan ELSEWHERE = new StatusSpan("elsewhere: ", Style.EMPTY);
    private static final List<Marker> STRONGEST_FIRST = List.of(Marker.values()).reversed();

    private StatusText() {}

    /** {@code counts} says how many live sessions of the project carry each marker; {@code elsewhere} is every place outside it. */
    static List<StatusSpan> spans(Map<Marker, Long> counts, List<ElsewhereGroupView> elsewhere) {
        List<List<StatusSpan>> own = STRONGEST_FIRST.stream()
                .filter(counts::containsKey)
                .map(marker -> List.of(new StatusSpan(
                        COUNT.formatted(SessionText.glyph(marker), counts.get(marker)),
                        Style.EMPTY.fg(SessionText.colour(marker)))))
                .toList();
        List<List<StatusSpan>> others = elsewhere.stream()
                .flatMap(group -> group.strongestWaiting().stream()
                        .map(marker -> List.of(
                                new StatusSpan(SessionText.glyph(marker), Style.EMPTY.fg(SessionText.colour(marker))),
                                new StatusSpan(PLACE.formatted(group.name().value()), Style.EMPTY))))
                .toList();
        return Stream.of(
                        joined(own),
                        others.isEmpty() || own.isEmpty() ? List.<StatusSpan>of() : List.of(SECTION_GAP),
                        others.isEmpty() ? List.<StatusSpan>of() : List.of(ELSEWHERE),
                        joined(others))
                .flatMap(List::stream)
                .toList();
    }

    private static List<StatusSpan> joined(List<List<StatusSpan>> items) {
        return IntStream.range(0, items.size())
                .boxed()
                .flatMap(index -> Stream.concat(index == 0 ? Stream.empty() : Stream.of(GAP), items.get(index).stream()))
                .toList();
    }
}

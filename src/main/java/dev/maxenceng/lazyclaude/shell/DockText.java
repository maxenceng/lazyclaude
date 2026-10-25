package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

/** How the shell words the dock: its title carries a tab per hosted session, the marker in colour then its name, or {@code starting}. */
final class DockText {

    private static final String DOCK = "dock";
    private static final String TABS_LEAD = " ─ ";
    private static final String TAB_SEPARATOR = " │ ";
    private static final String GAP = " ";
    private static final String STARTING = "starting";
    private static final String STRIP_LEAD = "dock: ";
    private static final String NO_TABS = "no hosted session";
    private static final String TAB_OPEN = "[";
    private static final String TAB_CLOSE = "]";
    private static final String STRIP_GAP = " ";

    private DockText() {}

    static Line title(List<HostedSessionView> tabs, int selected) {
        Stream<Span> labels = IntStream.range(0, tabs.size())
                .boxed()
                .flatMap(index -> Stream.concat(
                        Stream.of(Span.raw(index == 0 ? TABS_LEAD : TAB_SEPARATOR)),
                        tab(tabs.get(index), index == selected).stream()));
        return Line.from(Stream.concat(Stream.of(Span.raw(DOCK)), labels).toList());
    }

    /** The dock folded to one line: each tab as its marker and name in brackets, the selected one reversed. */
    static List<StatusSpan> strip(List<HostedSessionView> tabs, int selected) {
        Stream<StatusSpan> labels = tabs.isEmpty()
                ? Stream.of(new StatusSpan(NO_TABS, Style.EMPTY.dim()))
                : IntStream.range(0, tabs.size())
                        .boxed()
                        .flatMap(index -> stripTab(tabs.get(index), index == selected));
        return Stream.concat(Stream.of(new StatusSpan(STRIP_LEAD, Style.EMPTY.dim())), labels).toList();
    }

    private static Stream<StatusSpan> stripTab(HostedSessionView tab, boolean selected) {
        Style label = selected ? Style.EMPTY.reversed() : Style.EMPTY;
        Stream<StatusSpan> inside = tab.live()
                .map(live -> Stream.of(
                        new StatusSpan(SessionText.glyph(live.state()), Style.EMPTY.fg(SessionText.colour(live.state()))),
                        new StatusSpan(GAP + live.name().value(), label)))
                .orElseGet(() -> Stream.of(new StatusSpan(STARTING, label.dim())));
        return Stream.of(
                        Stream.of(new StatusSpan(TAB_OPEN, Style.EMPTY)),
                        inside,
                        Stream.of(new StatusSpan(TAB_CLOSE + STRIP_GAP, Style.EMPTY)))
                .flatMap(Function.identity());
    }

    private static List<Span> tab(HostedSessionView tab, boolean selected) {
        Style label = selected ? Style.EMPTY.reversed() : Style.EMPTY;
        return tab.live()
                .map(live -> List.of(glyph(live), Span.raw(GAP), Span.styled(live.name().value(), label)))
                .orElseGet(() -> List.of(Span.styled(STARTING, label.dim())));
    }

    private static Span glyph(LiveSessionView live) {
        return Span.styled(SessionText.glyph(live.state()), Style.EMPTY.fg(SessionText.colour(live.state())));
    }
}

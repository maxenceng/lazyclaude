package dev.maxenceng.lazyclaude.shell;

import java.util.List;
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

    private DockText() {}

    static Line title(List<HostedSessionView> tabs, int selected) {
        Stream<Span> labels = IntStream.range(0, tabs.size())
                .boxed()
                .flatMap(index -> Stream.concat(
                        Stream.of(Span.raw(index == 0 ? TABS_LEAD : TAB_SEPARATOR)),
                        tab(tabs.get(index), index == selected).stream()));
        return Line.from(Stream.concat(Stream.of(Span.raw(DOCK)), labels).toList());
    }

    private static List<Span> tab(HostedSessionView tab, boolean selected) {
        Style label = selected ? Style.EMPTY.reversed() : Style.EMPTY;
        return tab.live()
                .<List<Span>>map(live -> List.of(glyph(live), Span.raw(GAP), Span.styled(live.name().value(), label)))
                .orElseGet(() -> List.of(Span.styled(STARTING, label.dim())));
    }

    private static Span glyph(LiveSessionView live) {
        return Span.styled(SessionText.glyph(live.state()), Style.EMPTY.fg(SessionText.colour(live.state())));
    }
}

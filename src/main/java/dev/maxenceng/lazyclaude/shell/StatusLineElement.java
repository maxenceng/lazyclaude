package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** The top row: the strongest marker of the project's live sessions on the left in its colour, then the warning, if one was raised, flush right in the warning colour and cut with an ellipsis rather than wrapped. */
final class StatusLineElement implements Element {

    private static final String MARK = "⚠ ";
    private static final String COUNT_SEPARATOR = " · ";
    private static final int HEIGHT = 1;
    private static final int SINGLE = 1;
    private static final int MARKER_WIDTH = 2;

    private final Optional<Marker> marker;
    private final Optional<WarningView> warning;

    StatusLineElement(Optional<Marker> marker, Optional<WarningView> warning) {
        this.marker = marker;
        this.warning = warning;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (area.height() <= 0 || area.width() <= 0) {
            return;
        }
        marker.ifPresent(strongest ->
                frame.buffer().setString(area.x(), area.y(), SessionText.glyph(strongest), Style.EMPTY.fg(SessionText.colour(strongest))));
        if (warning.isEmpty()) {
            return;
        }
        int reserved = marker.isPresent() ? MARKER_WIDTH : 0;
        String label = RowsElement.fit(label(warning.get()), Math.max(0, area.width() - reserved));
        int x = area.x() + area.width() - label.codePointCount(0, label.length());
        frame.buffer().setString(x, area.y(), label, Style.EMPTY.fg(Color.YELLOW));
    }

    private static String label(WarningView view) {
        return view.count() > SINGLE ? MARK + view.count() + COUNT_SEPARATOR + view.text() : MARK + view.text();
    }

    @Override
    public Constraint constraint() {
        return Constraint.length(HEIGHT);
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }
}

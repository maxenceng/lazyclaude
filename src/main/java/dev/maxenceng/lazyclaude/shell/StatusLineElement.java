package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/**
 * The top row: the project's marker counts and the other projects waiting on you on the left, each in its colour and cut with
 * an ellipsis before the warning; then a question awaiting y/n in their place, else the warning, if one was raised, flush right in the
 * warning colour and cut with an ellipsis rather than wrapped.
 */
final class StatusLineElement implements Element {

    private static final String ELLIPSIS = "…";
    private static final String SINGLE_FORMAT = "⚠ %s";
    private static final String COUNTED_FORMAT = "⚠ %d · %s";
    private static final int HEIGHT = 1;
    private static final int SINGLE = 1;
    private static final int WARNING_GAP = 1;

    private final List<StatusSpan> spans;
    private final Optional<WarningView> warning;
    private final Optional<String> prompt;

    StatusLineElement(List<StatusSpan> spans, Optional<WarningView> warning, Optional<String> prompt) {
        this.spans = spans;
        this.warning = warning;
        this.prompt = prompt;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (Areas.hasNoRoom(area)) {
            return;
        }
        prompt.ifPresentOrElse(
                asked -> frame.buffer().setString(area.x(), area.y(), RowsElement.fit(asked, area.width()), Style.EMPTY.bold()),
                () -> renderStatus(frame, area));
    }

    private void renderStatus(Frame frame, Rect area) {
        Optional<String> label = warning.map(shown -> RowsElement.fit(label(shown), area.width()));
        int reserved = label.map(text -> text.codePointCount(0, text.length()) + WARNING_GAP).orElse(0);
        drawSpans(frame, area, Math.max(0, area.width() - reserved));
        label.ifPresent(text -> frame.buffer()
                .setString(
                        area.x() + area.width() - text.codePointCount(0, text.length()),
                        area.y(),
                        text,
                        Style.EMPTY.fg(Color.YELLOW)));
    }

    /** The spans in the first {@code budget} cells; when they do not fit, the last of those cells is an ellipsis. */
    private void drawSpans(Frame frame, Rect area, int budget) {
        int total = spans.stream().mapToInt(StatusSpan::width).sum();
        boolean cut = total > budget;
        int limit = cut ? Math.max(0, budget - ELLIPSIS.length()) : budget;
        IntStream.range(0, spans.size()).forEach(index -> {
            int offset = spans.subList(0, index).stream().mapToInt(StatusSpan::width).sum();
            if (offset < limit) {
                StatusSpan span = spans.get(index).clipped(limit - offset);
                frame.buffer().setString(area.x() + offset, area.y(), span.text(), span.style());
            }
        });
        if (cut && budget > 0) {
            frame.buffer().setString(area.x() + limit, area.y(), ELLIPSIS, Style.EMPTY);
        }
    }

    private static String label(WarningView view) {
        return view.count() > SINGLE
                ? COUNTED_FORMAT.formatted(view.count(), view.text())
                : SINGLE_FORMAT.formatted(view.text());
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

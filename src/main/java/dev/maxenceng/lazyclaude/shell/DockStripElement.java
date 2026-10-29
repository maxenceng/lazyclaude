package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** The one row the dock folds to while another pane is zoomed: its tabs and their markers, cut at the right edge. */
final class DockStripElement implements Element {

    private static final int HEIGHT = 1;

    private final List<StatusSpan> spans;
    private final Consumer<Rect> drawn;

    /** {@code drawn} is told the area the strip was drawn in, for the mouse to find its tabs in. */
    DockStripElement(List<StatusSpan> spans, Consumer<Rect> drawn) {
        this.spans = spans;
        this.drawn = drawn;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (Areas.hasNoRoom(area)) {
            return;
        }
        drawn.accept(area);
        IntStream.range(0, spans.size()).forEach(index -> {
            int offset = spans.subList(0, index).stream().mapToInt(StatusSpan::width).sum();
            if (offset < area.width()) {
                StatusSpan span = spans.get(index).clipped(area.width() - offset);
                frame.buffer().setString(area.x() + offset, area.y(), span.text(), span.style());
            }
        });
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

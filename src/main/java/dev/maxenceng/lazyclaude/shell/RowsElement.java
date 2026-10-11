package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.IntStream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** Draws lines top-down inside whatever area it gets, scrolled just far enough to show the selected one. */
final class RowsElement implements Element {

    private static final String ELLIPSIS = "…";
    private static final String PADDING = " ";

    private final List<RowLine> lines;
    private final int selected;
    private final boolean highlighted;

    RowsElement(List<RowLine> lines, int selected, boolean highlighted) {
        this.lines = lines;
        this.selected = selected;
        this.highlighted = highlighted;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        int height = area.height();
        if (height <= 0 || area.width() <= 0) {
            return;
        }
        int first = Math.max(0, Math.min(selected - height + 1, lines.size() - height));
        IntStream.range(0, Math.min(height, lines.size() - first))
                .forEach(row -> renderRow(frame, area, row, first + row));
    }

    private void renderRow(Frame frame, Rect area, int row, int index) {
        RowLine entry = lines.get(index);
        String line = fit(entry.whole(), area.width());
        boolean reversed = index == selected && highlighted;
        Style style = reversed ? Style.EMPTY.reversed() : Style.EMPTY;
        String padded = reversed ? line + PADDING.repeat(area.width() - line.codePointCount(0, line.length())) : line;
        frame.buffer().setString(area.x(), area.y() + row, padded, style);
        int gutterWidth = Math.min(entry.gutter().codePointCount(0, entry.gutter().length()), line.codePointCount(0, line.length()));
        if (gutterWidth > 0) {
            Style gutterStyle = reversed ? entry.gutterStyle().reversed() : entry.gutterStyle();
            frame.buffer().setString(area.x(), area.y() + row, line.substring(0, line.offsetByCodePoints(0, gutterWidth)), gutterStyle);
        }
    }

    // deferred: a wide character (CJK, emoji) counts as one cell, so such a title overruns its panel — close it when one shows up in a backlog.
    static String fit(String line, int width) {
        if (line.codePointCount(0, line.length()) <= width) {
            return line;
        }
        int end = line.offsetByCodePoints(0, width - 1);
        return line.substring(0, end) + ELLIPSIS;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }
}

package dev.maxenceng.lazyclaude.shell;

import java.util.List;

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

    private final List<String> lines;
    private final int selected;
    private final boolean highlighted;

    RowsElement(List<String> lines, int selected, boolean highlighted) {
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
        for (int row = 0; row < height && first + row < lines.size(); row++) {
            int index = first + row;
            String line = fit(lines.get(index), area.width());
            Style style = index == selected && highlighted ? Style.EMPTY.reversed() : Style.EMPTY;
            String padded = style == Style.EMPTY ? line : line + PADDING.repeat(area.width() - line.codePointCount(0, line.length()));
            frame.buffer().setString(area.x(), area.y() + row, padded, style);
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

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
    private static final int COLUMN_GAP = 1;

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
        if (Areas.noRoomIn(height) || Areas.noRoomIn(area.width())) {
            return;
        }
        int first = firstShown(selected, height, lines.size());
        IntStream.range(0, Math.min(height, lines.size() - first))
                .forEach(row -> renderRow(frame, area, row, first + row));
    }

    /** The index of the first line drawn when {@code height} lines are shown of {@code size}: just far enough on to show the selected one. */
    static int firstShown(int selected, int height, int size) {
        return Math.clamp(selected - height + 1, 0, Math.max(0, size - height));
    }

    private void renderRow(Frame frame, Rect area, int row, int index) {
        RowLine entry = lines.get(index);
        int columnWidth = columnWidth(entry, area.width());
        String line = fit(entry.whole(), area.width() - reserved(columnWidth));
        boolean reversed = index == selected && highlighted;
        Style style = reversed ? entry.textStyle().reversed() : entry.textStyle();
        String padded = reversed ? line + PADDING.repeat(area.width() - line.codePointCount(0, line.length())) : line;
        frame.buffer().setString(area.x(), area.y() + row, padded, style);
        int gutterWidth = Math.min(entry.gutter().codePointCount(0, entry.gutter().length()), line.codePointCount(0, line.length()));
        if (gutterWidth > 0) {
            Style gutterStyle = reversed ? entry.gutterStyle().reversed() : entry.gutterStyle();
            frame.buffer().setString(area.x(), area.y() + row, line.substring(0, line.offsetByCodePoints(0, gutterWidth)), gutterStyle);
        }
        IntStream.range(0, columnWidth)
                .forEach(cell -> renderCell(frame, area.x() + area.width() - columnWidth + cell, area.y() + row, entry.column().get(cell), reversed));
    }

    private static void renderCell(Frame frame, int x, int y, RowCell cell, boolean reversed) {
        frame.buffer().setString(x, y, cell.glyph(), reversed ? cell.style().reversed() : cell.style());
    }

    /** A column is drawn only when the panel is wide enough to keep a cell of the text before it. */
    private static int columnWidth(RowLine entry, int width) {
        return entry.column().size() + COLUMN_GAP < width ? entry.column().size() : 0;
    }

    private static int reserved(int columnWidth) {
        return columnWidth == 0 ? 0 : columnWidth + COLUMN_GAP;
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

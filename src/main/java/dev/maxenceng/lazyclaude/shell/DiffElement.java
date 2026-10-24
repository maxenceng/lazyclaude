package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.IntStream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/**
 * Draws the rows of a review's diff that fit the area, from the first one the pane scrolled to. It scrolls just far enough to keep
 * the cursor's line in view and looks at no row beyond the area, so a frame costs what the screen holds, not what the diff does.
 */
final class DiffElement implements Element {

    private final ReviewPane pane;

    DiffElement(ReviewPane pane) {
        this.pane = pane;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        DiffRows rows = pane.rows();
        if (Areas.hasNoRoom(area) || rows.isEmpty()) {
            return;
        }
        pane.observeHeight(area.height());
        pane.top(scrolled(rows, area));
        drawFrom(frame, area, rows, pane.top(), 0);
    }

    /** Draws rows from {@code row} on, each below the one before, until the area is full or the rows run out. */
    private void drawFrom(Frame frame, Rect area, DiffRows rows, int row, int y) {
        if (row < rows.size() && y < area.height()) {
            drawFrom(frame, area, rows, row + 1, y + draw(frame, area, rows, row, y));
        }
    }

    /** Draws a row from screen row {@code y} down, as much of it as fits; answers how many screen rows it takes. */
    private int draw(Frame frame, Rect area, DiffRows rows, int row, int y) {
        boolean cursor = rows.hasLines() && row == rows.rowOfLine(pane.cursor());
        boolean inRange = rows.lineOfRow(row).filter(pane::inRange).isPresent();
        List<List<StatusSpan>> lines = DiffText.rows(rows.row(row), area.width(), rows.numberWidth(), cursor, inRange);
        IntStream.range(0, Math.min(lines.size(), area.height() - y))
                .forEach(line -> drawSpans(frame, area, y + line, lines.get(line)));
        if (rows.row(row) instanceof BoxRow(CommentBox box) && !box.saving()) {
            placeCursor(frame, area, box, rows.numberWidth(), y);
        }
        return lines.size();
    }

    private static void placeCursor(Frame frame, Rect area, CommentBox box, int numberWidth, int y) {
        BoxCaret caret = BoxText.inlineCaret(box, area.width(), DiffText.gutterWidth(numberWidth));
        if (y + caret.row() < area.height()) {
            frame.setCursorPosition(area.x() + caret.column(), area.y() + y + caret.row());
        }
    }

    private static void drawSpans(Frame frame, Rect area, int y, List<StatusSpan> spans) {
        if (!spans.isEmpty()) {
            frame.buffer().setString(area.x(), area.y() + y, spans.getFirst().text(), spans.getFirst().style());
            drawSpans(frame, new Rect(area.x() + spans.getFirst().width(), area.y(), area.width(), area.height()), y, spans.subList(1, spans.size()));
        }
    }

    /** The first row to draw so that the cursor's line is whole in view: the one scrolled to if it is, else the nearest that shows it. */
    private int scrolled(DiffRows rows, Rect area) {
        if (!rows.hasLines()) {
            return 0;
        }
        int cursorRow = rows.rowOfLine(pane.cursor());
        int target = rows.boxRow().orElse(cursorRow);
        int top = Math.min(pane.top(), rows.size() - 1);
        if (cursorRow < top) {
            return withItsHeaders(rows, cursorRow);
        }
        return reaches(rows, area, top, target, 0) ? top : earliestStart(rows, area, target, heightOf(rows, area, target));
    }

    /** The row a line is scrolled up to: the file and hunk headers above it come along. */
    private static int withItsHeaders(DiffRows rows, int row) {
        return row > 0 && !isAttachedOrLine(rows.row(row - 1))
                ? withItsHeaders(rows, row - 1)
                : row;
    }

    private static boolean isAttachedOrLine(DiffRow row) {
        return row instanceof LineRow || row instanceof ThreadRow || row instanceof PendingRow || row instanceof BoxRow;
    }

    /** Whether rows {@code from} through {@code target} fit the area, looking at no more rows than the area is high. */
    private boolean reaches(DiffRows rows, Rect area, int from, int target, int used) {
        int next = used + heightOf(rows, area, from);
        if (next > area.height()) {
            return false;
        }
        return from == target || reaches(rows, area, from + 1, target, next);
    }

    /** The first row from which the rows down to {@code row} still fit the area. */
    private int earliestStart(DiffRows rows, Rect area, int row, int used) {
        if (row == 0) {
            return row;
        }
        int next = used + heightOf(rows, area, row - 1);
        return next > area.height() ? row : earliestStart(rows, area, row - 1, next);
    }

    private static int heightOf(DiffRows rows, Rect area, int row) {
        return DiffText.height(rows.row(row), area.width(), rows.numberWidth());
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

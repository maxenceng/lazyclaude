package dev.maxenceng.lazyclaude.shell;

import java.util.List;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** A column's rows scrolled to keep the selected one visible; when rows remain below, the last line says how many. */
final class MoreBelowElement implements Element {

    private static final String MORE = "↓ %d more";
    private static final int ONE_LINE = 1;

    private final List<RowLine> lines;
    private final int selected;
    private final boolean highlighted;

    MoreBelowElement(List<RowLine> lines, int selected, boolean highlighted) {
        this.lines = lines;
        this.selected = selected;
        this.highlighted = highlighted;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (Areas.hasNoRoom(area)) {
            return;
        }
        int rows = area.height() - ONE_LINE;
        int below = lines.size() - RowsElement.firstShown(selected, rows, lines.size()) - rows;
        if (area.height() <= ONE_LINE || lines.size() <= area.height() || below <= 0) {
            context.renderChild(new RowsElement(lines, selected, highlighted), frame, area);
            return;
        }
        context.renderChild(new RowsElement(lines, selected, highlighted), frame, new Rect(area.x(), area.y(), area.width(), rows));
        frame.buffer()
                .setString(area.x(), area.y() + rows, RowsElement.fit(MORE.formatted(below), area.width()), Style.EMPTY.dim());
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

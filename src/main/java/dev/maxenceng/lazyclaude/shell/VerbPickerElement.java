package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** Draws the verb picker as a box whose top-left corner is the Sessions panel's, over whatever is there, cut at the screen's edge. */
final class VerbPickerElement implements Element {

    private static final String TOP_LEFT = "╭";
    private static final String TOP_RIGHT = "╮";
    private static final String BOTTOM_LEFT = "╰";
    private static final String BOTTOM_RIGHT = "╯";
    private static final String RULE = "─";
    private static final String SIDE = "│";
    private static final String GAP = " ";
    private static final int PADDING = 2;

    private final VerbPicker picker;
    private final Supplier<Optional<Rect>> anchor;

    VerbPickerElement(VerbPicker picker, Supplier<Optional<Rect>> anchor) {
        this.picker = picker;
        this.anchor = anchor;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        Rect corner = anchor.get().orElse(area);
        int inner = VerbPickerText.width(picker) + PADDING * GAP.length();
        List<String> rows = VerbPickerText.rows(picker);
        Style border = Style.EMPTY.fg(Color.CYAN);
        draw(frame, area, corner, 0, ruled(TOP_LEFT, VerbPickerText.title(picker), TOP_RIGHT, inner), border);
        IntStream.range(0, rows.size())
                .forEach(row -> draw(frame, area, corner, row + 1, side(rows.get(row), inner),
                        row == picker.cursor() ? Style.EMPTY.bold() : Style.EMPTY));
        draw(frame, area, corner, rows.size() + 1, ruled(BOTTOM_LEFT, VerbPickerText.hint(), BOTTOM_RIGHT, inner), border);
    }

    private static String ruled(String left, String label, String right, int inner) {
        String text = GAP + label + GAP;
        return left + text + RULE.repeat(inner - text.length()) + right;
    }

    private static String side(String row, int inner) {
        String text = GAP + row;
        return SIDE + text + GAP.repeat(inner - text.length()) + SIDE;
    }

    private static void draw(Frame frame, Rect area, Rect corner, int row, String line, Style style) {
        int y = corner.y() + row;
        int room = area.x() + area.width() - corner.x();
        if (y < area.y() + area.height() && room > 0) {
            frame.buffer().setString(corner.x(), y, RowsElement.fit(line, room), style);
        }
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

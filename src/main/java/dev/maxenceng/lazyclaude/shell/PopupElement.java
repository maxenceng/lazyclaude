package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** Draws a popup as a box with a title, rows and a hint on its border, its top-left corner the anchor's, over whatever is there, cut at the screen's edge. */
final class PopupElement implements Element {

    private static final String TOP_LEFT = "╭";
    private static final String TOP_RIGHT = "╮";
    private static final String BOTTOM_LEFT = "╰";
    private static final String BOTTOM_RIGHT = "╯";
    private static final String RULE = "─";
    private static final String SIDE = "│";
    private static final String GAP = " ";
    private static final int PADDING = 2;

    private final String title;
    private final List<String> rows;
    private final Optional<Integer> bold;
    private final String hint;
    private final Supplier<Optional<Rect>> anchor;

    /** {@code bold} is the row the cursor is on, if there is a cursor. */
    PopupElement(String title, List<String> rows, Optional<Integer> bold, String hint, Supplier<Optional<Rect>> anchor) {
        this.title = title;
        this.rows = rows;
        this.bold = bold;
        this.hint = hint;
        this.anchor = anchor;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        Rect corner = anchor.get().orElse(area);
        int inner = width() + PADDING * GAP.length();
        Style border = Style.EMPTY.fg(Color.CYAN);
        draw(frame, area, corner, 0, ruled(TOP_LEFT, title, TOP_RIGHT, inner), border);
        IntStream.range(0, rows.size())
                .forEach(row -> draw(frame, area, corner, row + 1, side(rows.get(row), inner),
                        bold.filter(cursor -> cursor == row).isPresent() ? Style.EMPTY.bold() : Style.EMPTY));
        draw(frame, area, corner, rows.size() + 1, ruled(BOTTOM_LEFT, hint, BOTTOM_RIGHT, inner), border);
    }

    private int width() {
        return Stream.concat(Stream.of(title, hint), rows.stream())
                .mapToInt(text -> text.codePointCount(0, text.length()))
                .max()
                .orElse(0);
    }

    private static String ruled(String left, String label, String right, int inner) {
        String text = GAP + label + GAP;
        return left + text + RULE.repeat(inner - text.codePointCount(0, text.length())) + right;
    }

    private static String side(String row, int inner) {
        String text = GAP + row;
        return SIDE + text + GAP.repeat(inner - text.codePointCount(0, text.length())) + SIDE;
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

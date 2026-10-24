package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.published.CellColour;
import dev.maxenceng.lazyclaude.session.published.CursorAt;
import dev.maxenceng.lazyclaude.session.published.DefaultColour;
import dev.maxenceng.lazyclaude.session.published.IndexedColour;
import dev.maxenceng.lazyclaude.session.published.PastedText;
import dev.maxenceng.lazyclaude.session.published.RgbColour;
import dev.maxenceng.lazyclaude.session.published.RunStyle;
import dev.maxenceng.lazyclaude.session.published.ScreenCursor;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;
import dev.maxenceng.lazyclaude.session.published.StyledRun;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.session.published.TextAttribute;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Span;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.PasteEvent;

/**
 * Draws a hosted session's terminal screen cell for cell: each run where the session put it, in its colours and attributes,
 * cut at the edge of the area, and the session's cursor when the dock has focus. It is focusable only because TamboUI hands a
 * paste to the focused element alone.
 */
final class TerminalScreenElement implements Element {

    private static final String ID = "hosted-terminal";
    private static final int RGB_RED_SHIFT = 16;
    private static final int RGB_GREEN_SHIFT = 8;
    private static final int RGB_BYTE = 0xFF;

    private final TerminalScreen screen;
    private final boolean focused;
    private final Consumer<PastedText> onPaste;

    TerminalScreenElement(TerminalScreen screen, boolean focused, Consumer<PastedText> onPaste) {
        this.screen = screen;
        this.focused = focused;
        this.onPaste = onPaste;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (Areas.hasNoRoom(area)) {
            return;
        }
        IntStream.range(0, Math.min(area.height(), screen.rows().size()))
                .forEach(row -> drawRow(frame, area, row, screen.rows().get(row)));
        if (focused) {
            placeCursor(frame, area);
        }
    }

    private static void drawRow(Frame frame, Rect area, int row, ScreenRow line) {
        int[] column = {0};
        line.runs().forEach(run -> column[0] = drawRun(frame, area, row, column[0], run));
    }

    private static int drawRun(Frame frame, Rect area, int row, int column, StyledRun run) {
        int room = area.width() - column;
        if (room <= 0) {
            return column;
        }
        String text = fitted(run.text().value(), room);
        frame.buffer().setString(area.x() + column, area.y() + row, text, styleOf(run.style()));
        return column + Span.raw(text).width();
    }

    private static String fitted(String text, int room) {
        if (Span.raw(text).width() <= room) {
            return text;
        }
        int[] used = {0};
        return text.codePoints()
                .takeWhile(codePoint -> (used[0] += Span.raw(Character.toString(codePoint)).width()) <= room)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private void placeCursor(Frame frame, Rect area) {
        ScreenCursor cursor = screen.cursor();
        if (cursor instanceof CursorAt at
                && at.column().value() < area.width()
                && at.row().value() < area.height()) {
            frame.setCursorPosition(area.x() + at.column().value(), area.y() + at.row().value());
        }
    }

    private static Style styleOf(RunStyle style) {
        Style drawn = Style.EMPTY;
        if (!(style.foreground() instanceof DefaultColour)) {
            drawn = drawn.fg(colour(style.foreground()));
        }
        if (!(style.background() instanceof DefaultColour)) {
            drawn = drawn.bg(colour(style.background()));
        }
        return withAttributes(drawn, style.attributes());
    }

    private static Style withAttributes(Style base, List<TextAttribute> attributes) {
        return attributes.stream().reduce(base, TerminalScreenElement::with, (first, second) -> second);
    }

    private static Style with(Style drawn, TextAttribute attribute) {
        return switch (attribute) {
            case BOLD -> drawn.bold();
            case DIM -> drawn.dim();
            case ITALIC -> drawn.italic();
            case UNDERLINED -> drawn.underlined();
            case REVERSED -> drawn.reversed();
        };
    }

    private static Color colour(CellColour colour) {
        return switch (colour) {
            case DefaultColour _ -> Color.RESET;
            case IndexedColour indexed -> Color.indexed(indexed.index());
            case RgbColour rgb ->
                Color.rgb(
                        (rgb.rgb() >> RGB_RED_SHIFT) & RGB_BYTE,
                        (rgb.rgb() >> RGB_GREEN_SHIFT) & RGB_BYTE,
                        rgb.rgb() & RGB_BYTE);
        };
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public EventResult handlePasteEvent(PasteEvent event) {
        onPaste.accept(new PastedText(event.text()));
        return EventResult.HANDLED;
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

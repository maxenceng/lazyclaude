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

/**
 * Draws one session per line, top-down inside whatever area it gets, scrolled just far enough to show the
 * selected one: a live session as its state glyph in its colour, its name and its age against the right edge; a
 * past one as a dim {@code ○}, its title and the age since its last activity; and the row of older ones, dim.
 */
final class SessionRowsElement implements Element {

    private static final String GAP = " ";

    private final List<SessionRow> sessions;
    private final int selected;
    private final boolean highlighted;

    SessionRowsElement(List<SessionRow> sessions, int selected, boolean highlighted) {
        this.sessions = sessions;
        this.selected = selected;
        this.highlighted = highlighted;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        int height = area.height();
        if (height <= 0 || area.width() <= 0) {
            return;
        }
        int first = Math.max(0, Math.min(selected - height + 1, sessions.size() - height));
        IntStream.range(0, Math.min(height, sessions.size() - first))
                .forEach(row -> draw(frame, area, row, sessions.get(first + row), first + row == selected && highlighted));
    }

    private static void draw(Frame frame, Rect area, int row, SessionRow session, boolean reversed) {
        Style plain = reversed ? Style.EMPTY.reversed() : Style.EMPTY;
        int y = area.y() + row;
        switch (session) {
            case LiveSessionRow live -> {
                String glyph = SessionText.glyph(live.session().state());
                line(frame, area, y, glyph, plain.fg(SessionText.colour(live.session().state())), live.session().name().value(), plain, live.session().age().label(), plain);
            }
            case PastSessionRow past -> {
                Style title = SessionText.titleIsStandIn(past.session()) ? plain.dim() : plain;
                line(frame, area, y, SessionText.pastGlyph(), plain.dim(), SessionText.title(past.session()), title, past.session().age().label(), plain);
            }
            case OlderSessionsRow older ->
                line(frame, area, y, SessionText.olderGlyph(), plain.dim(), SessionText.olderLabel(older), plain.dim(), "", plain);
        }
    }

    private static void line(
            Frame frame,
            Rect area,
            int y,
            String glyph,
            Style glyphStyle,
            String text,
            Style textStyle,
            String age,
            Style ageStyle) {
        int ageWidth = age.isEmpty() ? 0 : GAP.length() + age.length();
        int textWidth = Math.max(0, area.width() - glyph.length() - GAP.length() - ageWidth);
        String fitted = textWidth == 0 ? "" : RowsElement.fit(text, textWidth);
        String padding = " ".repeat(Math.max(0, textWidth - fitted.codePointCount(0, fitted.length())));
        frame.buffer().setString(area.x(), y, glyph, glyphStyle);
        frame.buffer().setString(area.x() + glyph.length(), y, GAP + fitted + padding, textStyle);
        if (ageWidth > 0) {
            frame.buffer().setString(area.x() + area.width() - age.length() - GAP.length(), y, GAP + age, ageStyle);
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

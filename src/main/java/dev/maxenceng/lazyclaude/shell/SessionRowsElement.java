package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionView;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/**
 * Draws one session per line, top-down inside whatever area it gets, scrolled just far enough to show the
 * selected one: the state glyph in its colour, the name, and the age against the right edge.
 */
final class SessionRowsElement implements Element {

    private static final String GAP = " ";

    private final List<SessionView> sessions;
    private final int selected;
    private final boolean highlighted;

    SessionRowsElement(List<SessionView> sessions, int selected, boolean highlighted) {
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

    private static void draw(Frame frame, Rect area, int row, SessionView session, boolean reversed) {
        String glyph = SessionText.glyph(session.state());
        String age = session.age().label();
        int nameWidth = Math.max(0, area.width() - glyph.length() - GAP.length() - GAP.length() - age.length());
        String name = nameWidth == 0 ? "" : RowsElement.fit(session.name().value(), nameWidth);
        String padding = " ".repeat(Math.max(0, nameWidth - name.codePointCount(0, name.length())));
        Style plain = reversed ? Style.EMPTY.reversed() : Style.EMPTY;
        Style glyphStyle = plain.fg(SessionText.colour(session.state()));
        int y = area.y() + row;
        frame.buffer().setString(area.x(), y, glyph, glyphStyle);
        frame.buffer().setString(area.x() + glyph.length(), y, GAP + name + padding + GAP + age, plain);
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

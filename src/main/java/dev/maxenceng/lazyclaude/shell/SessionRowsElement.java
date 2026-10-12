package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
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
    private static final int VERB_GAP = 2;

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
        int verbWidth = verbWidth(sessions);
        IntStream.range(0, Math.min(height, sessions.size() - first))
                .forEach(row -> draw(frame, area, row, sessions.get(first + row), first + row == selected && highlighted, verbWidth));
    }

    private static void draw(Frame frame, Rect area, int row, SessionRow session, boolean reversed, int verbWidth) {
        Style plain = reversed ? Style.EMPTY.reversed() : Style.EMPTY;
        int y = area.y() + row;
        switch (session) {
            case LiveSessionRow live -> {
                SessionState state = live.session().state();
                line(frame, area, y, SessionText.glyph(state), plain.fg(SessionText.colour(state)),
                        middle(live.link(), live.session().name().value(), plain, plain, verbWidth),
                        live.session().isHosted() ? SessionText.paneLabel() : live.session().age().label(), plain);
            }
            case PastSessionRow past -> {
                Style title = SessionText.titleIsStandIn(past.session()) ? plain.dim() : plain;
                line(frame, area, y, SessionText.pastGlyph(), plain.dim(),
                        middle(past.link(), SessionText.title(past.session()), plain, title, verbWidth), past.session().age().label(), plain);
            }
            case OlderSessionsRow older ->
                line(frame, area, y, SessionText.olderGlyph(), plain.dim(),
                        new MiddleText("", plain, SessionText.olderLabel(older), plain.dim(), ""), "", plain);
            case SectionHeaderRow header ->
                frame.buffer().setString(area.x(), y, RowsElement.fit(header.label(), area.width()), Style.EMPTY.dim());
            case NoSessionRow none -> {
                String label = RowsElement.fit(SessionText.noSessionLabel(), area.width());
                String padded = label + " ".repeat(Math.max(0, area.width() - label.codePointCount(0, label.length())));
                frame.buffer().setString(area.x(), y, padded, plain.dim());
            }
        }
    }

    private static MiddleText middle(Optional<TicketLink> link, String title, Style plain, Style titleStyle, int verbWidth) {
        String verb = SessionText.verb(link).map(found -> found + " ".repeat(verbWidth - found.length() + VERB_GAP)).orElse("");
        String suffix = SessionText.isWeak(link) ? GAP + SessionText.branchSuffix() : "";
        return new MiddleText(verb, plain, title, titleStyle, suffix);
    }

    private static int verbWidth(List<SessionRow> rows) {
        return rows.stream()
                .map(row -> switch (row) {
                    case LiveSessionRow live -> SessionText.verb(live.link());
                    case PastSessionRow past -> SessionText.verb(past.link());
                    default -> Optional.<String>empty();
                })
                .flatMap(Optional::stream)
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    // deferred: a panel narrower than verb and branch suffix draws both whole and the age overwrites them — close it when someone runs a panel that narrow.
    private static void line(Frame frame, Rect area, int y, String glyph, Style glyphStyle, MiddleText middle, String age, Style ageStyle) {
        int ageWidth = age.isEmpty() ? 0 : GAP.length() + age.length();
        int textWidth = Math.max(0, area.width() - glyph.length() - GAP.length() - ageWidth);
        int titleWidth = Math.max(0, textWidth - middle.verb().length() - middle.suffix().length());
        String fitted = titleWidth == 0 ? "" : RowsElement.fit(middle.title(), titleWidth);
        String padding = " ".repeat(Math.max(0, titleWidth - fitted.codePointCount(0, fitted.length())));
        int x = area.x() + glyph.length();
        frame.buffer().setString(area.x(), y, glyph, glyphStyle);
        frame.buffer().setString(x, y, GAP, middle.titleStyle());
        x += GAP.length();
        frame.buffer().setString(x, y, middle.verb(), middle.verbStyle());
        x += middle.verb().length();
        frame.buffer().setString(x, y, fitted, middle.titleStyle());
        x += fitted.codePointCount(0, fitted.length());
        frame.buffer().setString(x, y, middle.suffix(), middle.titleStyle().dim());
        x += middle.suffix().length();
        frame.buffer().setString(x, y, padding, middle.titleStyle());
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

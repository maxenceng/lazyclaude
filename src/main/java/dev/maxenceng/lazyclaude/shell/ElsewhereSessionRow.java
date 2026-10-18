package dev.maxenceng.lazyclaude.shell;

import java.util.List;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.published.PlaceName;
import dev.tamboui.style.Style;

/** A live session outside the project: its state glyph in its colour, then its name. */
record ElsewhereSessionRow(PlaceName place, LiveSessionView session) implements ElsewhereRow {

    private static final String GUTTER = "%s%s ";

    @Override
    public RowKey key() {
        return new RowKey(RowKind.SESSION, session.id().value());
    }

    @Override
    public boolean selectable() {
        return true;
    }

    @Override
    public RowLine line() {
        return new RowLine(
                GUTTER.formatted(Indent.STEP, SessionText.glyph(session.state())),
                Style.EMPTY.fg(SessionText.colour(session.state())),
                session.name().value(),
                Style.EMPTY,
                List.of());
    }
}

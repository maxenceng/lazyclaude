package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.tamboui.style.Style;

/**
 * A ticket under its status's header: id, then title, with the strongest marker of its live sessions in the gutter before the id and the
 * glyphs of its pull request in the column at the right edge.
 */
record TicketRow(TicketView ticket) implements Row {

    private static final String GUTTER_GAP = " ";

    @Override
    public RowKey key() {
        return new RowKey(RowKind.TICKET, ticket.id().value());
    }

    @Override
    public String text() {
        return "%s%s %s".formatted(Indent.STEP, ticket.id().value(), TicketText.title(ticket));
    }

    RowLine line(Optional<Marker> marker, List<RowCell> column) {
        return marker
                .map(strongest -> new RowLine(
                        SessionText.glyph(strongest) + GUTTER_GAP,
                        Style.EMPTY.fg(SessionText.colour(strongest)),
                        "%s %s".formatted(ticket.id().value(), TicketText.title(ticket)),
                        column))
                .orElseGet(() -> new RowLine("", Style.EMPTY, text(), column));
    }
}

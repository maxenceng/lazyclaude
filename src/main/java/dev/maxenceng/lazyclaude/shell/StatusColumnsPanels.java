package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.row;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.StatusGroupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketView;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.tamboui.toolkit.element.Element;

/** The Board's status columns across the screen: one panel per status, a narrow one for a finished status, then one for the unreadable files. */
final class StatusColumnsPanels {

    private static final String COLUMN_TITLE = "%s (%d)";
    private static final String NARROW_TITLE = "%s %d";
    private static final String UNREADABLE_LINE = "%s — %s";
    private static final int NARROW_WIDTH = 12;
    private static final int NO_SELECTION = -1;

    private StatusColumnsPanels() {}

    /**
     * {@code cursor} is the id of the ticket the cursor stands on; {@code line} words a ticket as its board row does; {@code drawn} is told
     * which rows each column that shares the width drew, and which column that is.
     */
    static Element of(
            StatusColumns columns,
            Optional<TicketId> cursor,
            Function<TicketView, RowLine> line,
            ObjIntConsumer<RowsDrawn> drawn) {
        Optional<StatusColumnsPlace> at = cursor.flatMap(columns::locate);
        List<StatusGroupView> shared = columns.shared();
        Stream<Element> sharing = IntStream.range(0, shared.size()).mapToObj(index -> sharing(shared.get(index), at, index, line, drawn));
        Stream<Element> narrow = columns.narrow().stream().map(StatusColumnsPanels::narrow);
        Stream<Element> unreadable = Optional.of(columns.board().unreadable())
                .filter(files -> !files.isEmpty())
                .stream()
                .map(StatusColumnsPanels::unreadable);
        return row(Stream.of(sharing, narrow, unreadable).flatMap(Function.identity()).toArray(Element[]::new))
                .fill();
    }

    private static Element sharing(
            StatusGroupView group,
            Optional<StatusColumnsPlace> at,
            int index,
            Function<TicketView, RowLine> line,
            ObjIntConsumer<RowsDrawn> drawn) {
        Optional<Integer> row = at.filter(cursor -> cursor.column() == index).map(StatusColumnsPlace::row);
        Element body = new MoreBelowElement(
                group.tickets().stream().map(line).toList(), row.orElse(NO_SELECTION), row.isPresent(), rows -> drawn.accept(rows, index));
        return Frames.framed(COLUMN_TITLE.formatted(group.status().label(), group.tickets().size()), body, row.isPresent())
                .titleEllipsis()
                .fill();
    }

    private static Element narrow(StatusGroupView group) {
        return Frames.framed(NARROW_TITLE.formatted(group.status().label(), group.tickets().size()), column(), false)
                .titleEllipsis()
                .length(NARROW_WIDTH);
    }

    private static Element unreadable(List<UnreadableTicketView> files) {
        List<RowLine> lines = files.stream()
                .map(file -> RowLine.plain(UNREADABLE_LINE.formatted(file.file().value(), file.reason().value())))
                .toList();
        return Frames.framed(COLUMN_TITLE.formatted(BoardRows.UNREADABLE_GROUP, files.size()), new MoreBelowElement(lines, NO_SELECTION, false), false)
                .titleEllipsis()
                .fill();
    }
}

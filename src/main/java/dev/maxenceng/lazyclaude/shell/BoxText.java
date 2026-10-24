package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.Stream;

import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.input.TextAreaState.DisplayRow;

/** How the review words a comment box: the text wrapped to the room it has, its frame, and the saving or not-saved line under it. */
final class BoxText {

    static final String HINT = "^s save  esc cancel";

    private static final String TOP_LEFT = "╭";
    private static final String TOP_RIGHT = "╮";
    private static final String BOTTOM_LEFT = "╰";
    private static final String BOTTOM_RIGHT = "╯";
    private static final String RULE = "─";
    private static final String SIDE = "│";
    private static final String SPACE = " ";
    private static final String SAVING = "saving…";
    private static final String NOT_SAVED = "not saved (%s)";
    private static final int FRAME = 4;
    private static final int CORNERS = 2;
    private static final int STATUS_INDENT = 2;
    private static final int SIDE_AND_GAP = 2;
    private static final int MIN_INNER = 8;

    private BoxText() {}

    /** The room the text has inside a box drawn across {@code width} cells. */
    static int innerWidth(int width) {
        return Math.max(MIN_INNER, width - FRAME);
    }

    /** The wrapped rows of the box's text, padded to {@code inner} cells. */
    static List<String> textRows(CommentBox box, int inner) {
        TextAreaState area = box.area();
        return area.computeDisplayRows(inner, Overflow.WRAP_CHARACTER).stream()
                .map(row -> padded(area.getLine(row.logicalRow()).substring(row.startCol(), row.endCol()), inner))
                .toList();
    }

    /** The row and column the cursor is drawn at within the wrapped text. */
    static BoxCaret caret(CommentBox box, int inner) {
        TextAreaState area = box.area();
        List<DisplayRow> rows = area.computeDisplayRows(inner, Overflow.WRAP_CHARACTER);
        int index = TextAreaState.findDisplayRowIndex(rows, area.cursorRow(), area.cursorCol());
        return new BoxCaret(index, Math.min(inner - 1, area.cursorCol() - rows.get(index).startCol()));
    }

    /** What stands under the box: {@code saving…} while a save is out, why the last one failed, else nothing. */
    static List<String> status(CommentBox box) {
        return box.saving()
                ? List.of(SAVING)
                : box.notSaved().map(reason -> List.of(NOT_SAVED.formatted(reason.value()))).orElse(List.of());
    }

    /** The screen rows of a box drawn inline, set in past {@code indent} cells across {@code width}. */
    static List<List<StatusSpan>> inline(CommentBox box, int width, int indent) {
        int boxWidth = Math.max(MIN_INNER + FRAME, width - indent);
        int inner = innerWidth(boxWidth);
        Style border = Style.EMPTY.fg(PendingText.COLOUR);
        String lead = SPACE.repeat(indent);
        Stream<List<StatusSpan>> top = Stream.of(List.of(
                new StatusSpan(lead, Style.EMPTY), new StatusSpan(TOP_LEFT + RULE.repeat(boxWidth - CORNERS) + TOP_RIGHT, border)));
        Stream<List<StatusSpan>> text = textRows(box, inner).stream()
                .map(row -> List.of(
                        new StatusSpan(lead, Style.EMPTY),
                        new StatusSpan(SIDE + SPACE, border),
                        new StatusSpan(row, Style.EMPTY),
                        new StatusSpan(SPACE + SIDE, border)));
        String hint = SPACE + HINT + SPACE;
        Stream<List<StatusSpan>> bottom = Stream.of(List.of(
                new StatusSpan(lead, Style.EMPTY),
                new StatusSpan(
                        BOTTOM_LEFT + hint + RULE.repeat(Math.max(0, boxWidth - CORNERS - hint.length())) + BOTTOM_RIGHT, border)));
        Stream<List<StatusSpan>> status = status(box).stream()
                .map(line -> List.of(new StatusSpan(lead + SPACE.repeat(STATUS_INDENT) + line, Style.EMPTY.dim())));
        return Stream.of(top, text, bottom, status).flatMap(rows -> rows).toList();
    }

    /** The screen column and row, within the inline box, of its cursor. */
    static BoxCaret inlineCaret(CommentBox box, int width, int indent) {
        BoxCaret within = caret(box, innerWidth(Math.max(MIN_INNER + FRAME, width - indent)));
        return new BoxCaret(within.row() + 1, indent + SIDE_AND_GAP + within.column());
    }

    private static String padded(String text, int inner) {
        return text + SPACE.repeat(Math.max(0, inner - text.codePointCount(0, text.length())));
    }

    static int rowCount(CommentBox box, int width, int indent) {
        return inline(box, width, indent).size();
    }

    /**
     * The rows a popup shows for a box within {@code room} rows: {@code above} first, then a rule, then the box's text and what stands under
     * it, the top ones dropped when they do not all fit so the caret stays in view.
     */
    static List<String> popupRows(CommentBox box, int inner, List<String> above, int room) {
        List<String> rows = allPopupRows(box, inner, above);
        int top = popupTop(rows, caretAmong(box, inner, above.size()), room);
        return rows.subList(top, Math.min(rows.size(), top + room));
    }

    /** The cursor within the rows {@link #popupRows} shows. */
    static BoxCaret popupCaret(CommentBox box, int inner, List<String> above, int room) {
        BoxCaret caret = caretAmong(box, inner, above.size());
        return new BoxCaret(caret.row() - popupTop(allPopupRows(box, inner, above), caret, room), caret.column());
    }

    private static List<String> allPopupRows(CommentBox box, int inner, List<String> above) {
        return Stream.of(above.stream(), Stream.of(RULE.repeat(inner)), textRows(box, inner).stream(), status(box).stream())
                .flatMap(rows -> rows)
                .toList();
    }

    private static BoxCaret caretAmong(CommentBox box, int inner, int above) {
        BoxCaret within = caret(box, inner);
        return new BoxCaret(above + 1 + within.row(), within.column());
    }

    /** The first row shown: as many of the last rows as fit, but never past the caret. */
    private static int popupTop(List<String> rows, BoxCaret caret, int room) {
        return Math.clamp(rows.size() - room, 0, caret.row());
    }
}

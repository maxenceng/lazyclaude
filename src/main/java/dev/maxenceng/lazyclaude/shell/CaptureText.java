package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Reason;

/** How the Board words the popup a new ticket's title is typed in: the title in the room it has, and the creating or not-created line under it. */
final class CaptureText {

    static final String TITLE = "New ticket";
    static final String HINT = "enter create · esc cancel";

    private static final String LABEL = "Title: ";
    private static final String CREATING = "creating…";
    private static final String NOT_CREATED = "⚠ %s";
    private static final int ROOM = 40;
    private static final int CURSOR_CELL = 1;
    private static final int TITLE_ROW = 0;

    private CaptureText() {}

    /** The title row, then what stands under it. */
    static List<String> rows(CaptureBox box) {
        return Stream.concat(Stream.of(LABEL + window(box)), status(box).stream()).toList();
    }

    static Optional<BoxCaret> caret(CaptureBox box) {
        return box.creating() ? Optional.empty() : Optional.of(new BoxCaret(TITLE_ROW, LABEL.length() + box.cursor() - start(box)));
    }

    /** The key bar while the popup is open. */
    static List<String> keys() {
        return List.of(HINT);
    }

    /** The part of the title that fits, keeping the cursor in it. */
    private static String window(CaptureBox box) {
        String text = box.content();
        int start = start(box);
        return text.substring(start, Math.min(text.length(), start + ROOM));
    }

    private static int start(CaptureBox box) {
        return Math.max(0, box.cursor() + CURSOR_CELL - ROOM);
    }

    private static List<String> status(CaptureBox box) {
        return box.creating()
                ? List.of(CREATING)
                : box.notCreated().map(CaptureText::notCreated).orElse(List.of());
    }

    private static List<String> notCreated(Reason reason) {
        return Wrapping.lines(NOT_CREATED.formatted(reason.value()), LABEL.length() + ROOM);
    }
}

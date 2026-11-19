package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** How the Board words the popup a new ticket's description is typed in: the description in the room it has, and the starting or not-started line under it. */
final class CaptureText {

    static final String TITLE = "New ticket";
    static final String HINT = "enter start · esc cancel";
    static final String BLANK = "describe the ticket first";

    private static final String LABEL = "Describe: ";
    private static final String STARTING = "starting…";
    private static final String NOT_STARTED = "⚠ %s";
    private static final int ROOM = 37;
    private static final int CURSOR_CELL = 1;
    private static final int DESCRIPTION_ROW = 0;

    private CaptureText() {}

    /** The description row, then what stands under it. */
    static List<String> rows(CaptureBox box) {
        return Stream.concat(Stream.of(LABEL + window(box)), status(box).stream()).toList();
    }

    static Optional<BoxCaret> caret(CaptureBox box) {
        return box.starting() ? Optional.empty() : Optional.of(new BoxCaret(DESCRIPTION_ROW, LABEL.length() + box.cursor() - start(box)));
    }

    /** The key bar while the popup is open. */
    static List<String> keys() {
        return List.of(HINT);
    }

    /** The part of the description that fits, keeping the cursor in it. */
    private static String window(CaptureBox box) {
        String text = box.content();
        int start = start(box);
        return text.substring(start, Math.min(text.length(), start + ROOM));
    }

    private static int start(CaptureBox box) {
        return Math.max(0, box.cursor() + CURSOR_CELL - ROOM);
    }

    private static List<String> status(CaptureBox box) {
        return box.starting()
                ? List.of(STARTING)
                : box.notStarted().map(CaptureText::notStarted).orElse(List.of());
    }

    private static List<String> notStarted(Reason reason) {
        return Wrapping.lines(NOT_STARTED.formatted(reason.value()), LABEL.length() + ROOM);
    }
}

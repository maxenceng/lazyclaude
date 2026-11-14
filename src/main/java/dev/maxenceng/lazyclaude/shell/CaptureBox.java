package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextInputState;

/**
 * The popup a new ticket's description is typed in: the text and cursor, and where it stands in starting the session that creates
 * it. A start in flight holds the box still; one that failed keeps the description and says why.
 */
final class CaptureBox {

    private final TextInputState description = new TextInputState();
    private boolean starting;
    private Optional<Reason> notStarted = Optional.empty();

    String content() {
        return description.text();
    }

    int cursor() {
        return description.cursorPosition();
    }

    boolean starting() {
        return starting;
    }

    Optional<Reason> notStarted() {
        return notStarted;
    }

    void startBegun() {
        starting = true;
        notStarted = Optional.empty();
    }

    void startRefused(Reason reason) {
        starting = false;
        notStarted = Optional.of(reason);
    }

    /** Applies a key to the description: a character, a deletion or a move of the cursor; {@code false} for any other key. */
    boolean edit(KeyEvent key) {
        if (starting || key.hasCtrl() || key.hasAlt()) {
            return false;
        }
        switch (key.code()) {
            case CHAR -> description.insert(Character.toString(key.codePoint()));
            case BACKSPACE -> description.deleteBackward();
            case DELETE -> description.deleteForward();
            case LEFT -> description.moveCursorLeft();
            case RIGHT -> description.moveCursorRight();
            case HOME -> description.moveCursorToStart();
            case END -> description.moveCursorToEnd();
            default -> {
                return false;
            }
        }
        return true;
    }
}

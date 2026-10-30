package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextInputState;

/**
 * The popup a new ticket's title is typed in: the text and cursor, and where it stands in creating the issue. A create in flight
 * holds the box still; one that failed keeps the title and says why.
 */
final class CaptureBox {

    private final TextInputState title = new TextInputState();
    private boolean creating;
    private Optional<Reason> notCreated = Optional.empty();

    String content() {
        return title.text();
    }

    int cursor() {
        return title.cursorPosition();
    }

    boolean creating() {
        return creating;
    }

    Optional<Reason> notCreated() {
        return notCreated;
    }

    void createBegun() {
        creating = true;
        notCreated = Optional.empty();
    }

    void createRefused(Reason reason) {
        creating = false;
        notCreated = Optional.of(reason);
    }

    /** Applies a key to the title: a character, a deletion or a move of the cursor; {@code false} for any other key. */
    boolean edit(KeyEvent key) {
        if (creating || key.hasCtrl() || key.hasAlt()) {
            return false;
        }
        switch (key.code()) {
            case CHAR -> title.insert(Character.toString(key.codePoint()));
            case BACKSPACE -> title.deleteBackward();
            case DELETE -> title.deleteForward();
            case LEFT -> title.moveCursorLeft();
            case RIGHT -> title.moveCursorRight();
            case HOME -> title.moveCursorToStart();
            case END -> title.moveCursorToEnd();
            default -> {
                return false;
            }
        }
        return true;
    }
}

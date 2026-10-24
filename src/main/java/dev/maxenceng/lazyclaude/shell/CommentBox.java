package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;

import dev.maxenceng.lazyclaude.review.published.Reason;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextAreaState;

/**
 * The box a comment or the summary is written in: its text and cursor, what it writes, and where it stands in saving. A save in
 * flight holds the box still; one that failed keeps the text and says why.
 */
final class CommentBox {

    private static final String NO_TEXT = "";
    private static final String NEWLINE = "\n";

    private final BoxTarget target;
    private final Optional<Integer> inlineAt;
    private final TextAreaState text;
    private boolean saving;
    private Optional<Reason> notSaved = Optional.empty();

    /** An empty box; {@code inlineAt} is the diff line the box is drawn under, or none for a box drawn as a popup. */
    CommentBox(BoxTarget target, Optional<Integer> inlineAt) {
        this(target, inlineAt, NO_TEXT);
    }

    /** {@code inlineAt} is the diff line the box is drawn under, or none for a box drawn as a popup. */
    CommentBox(BoxTarget target, Optional<Integer> inlineAt, String initial) {
        this.target = target;
        this.inlineAt = inlineAt;
        this.text = new TextAreaState(initial);
        this.text.moveCursorToEnd();
    }

    BoxTarget target() {
        return target;
    }

    Optional<Integer> inlineAt() {
        return inlineAt;
    }

    TextAreaState area() {
        return text;
    }

    String content() {
        return text.text();
    }

    boolean saving() {
        return saving;
    }

    Optional<Reason> notSaved() {
        return notSaved;
    }

    void saveBegun() {
        saving = true;
        notSaved = Optional.empty();
    }

    void saveRefused(Reason reason) {
        saving = false;
        notSaved = Optional.of(reason);
    }

    /** Applies a key to the text: a character, a newline, a deletion or a move of the cursor; {@code false} for any other key. */
    boolean edit(KeyEvent key) {
        if (saving || key.hasCtrl() || key.hasAlt()) {
            return false;
        }
        switch (key.code()) {
            case CHAR -> text.insert(String.valueOf(key.character()));
            case ENTER -> text.insert(NEWLINE);
            case BACKSPACE -> text.deleteBackward();
            case DELETE -> text.deleteForward();
            case LEFT -> text.moveCursorLeft();
            case RIGHT -> text.moveCursorRight();
            case UP -> text.moveCursorUp();
            case DOWN -> text.moveCursorDown();
            case HOME -> text.moveCursorToLineStart();
            case END -> text.moveCursorToLineEnd();
            default -> {
                return false;
            }
        }
        return true;
    }
}

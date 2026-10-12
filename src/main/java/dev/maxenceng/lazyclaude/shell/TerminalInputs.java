package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.KeyInput;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.PointerButton;
import dev.maxenceng.lazyclaude.session.published.PressedKey;
import dev.maxenceng.lazyclaude.session.published.TerminalKey;
import dev.maxenceng.lazyclaude.session.published.TerminalMouse;
import dev.maxenceng.lazyclaude.session.published.TypedCharacter;
import dev.tamboui.layout.Rect;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

/**
 * Maps what TamboUI parsed from the outer terminal to the published values a hosted session is fed, so no TamboUI type
 * crosses into {@code session}.
 */
final class TerminalInputs {

    private TerminalInputs() {}

    // deferred: Ctrl+J, Shift+Enter, Ctrl+Enter and Alt+Enter are lost inside the pane, since TamboUI collapses them to Enter or an unknown key before the shell sees them — close it when TamboUI supports the kitty keyboard protocol (upstream #446).
    static Optional<TerminalKey> key(KeyEvent event) {
        return input(event).map(input -> TerminalKey.builder()
                .input(input)
                .modifiers(modifiers(event.modifiers()))
                .build());
    }

    /** The mouse event as a cell of {@code pane}, counted from its top left, or nothing when it happened outside the pane. */
    static Optional<TerminalMouse> mouse(MouseEvent event, Rect pane) {
        int column = event.x() - pane.x();
        int row = event.y() - pane.y();
        if (column < 0 || row < 0 || column >= pane.width() || row >= pane.height()) {
            return Optional.empty();
        }
        return Optional.of(TerminalMouse.builder()
                .action(action(event))
                .button(button(event.button()))
                .column(new CellColumn(column))
                .row(new CellRow(row))
                .modifiers(modifiers(event.modifiers()))
                .build());
    }

    private static Optional<KeyInput> input(KeyEvent event) {
        if (event.code() == KeyCode.CHAR) {
            return Optional.of(new TypedCharacter(event.codePoint()));
        }
        return named(event.code()).map(PressedKey::new);
    }

    private static Optional<NamedKey> named(KeyCode code) {
        return switch (code) {
            case ENTER -> Optional.of(NamedKey.ENTER);
            case ESCAPE -> Optional.of(NamedKey.ESCAPE);
            case BACKSPACE -> Optional.of(NamedKey.BACKSPACE);
            case TAB -> Optional.of(NamedKey.TAB);
            case DELETE -> Optional.of(NamedKey.DELETE);
            case INSERT -> Optional.of(NamedKey.INSERT);
            case UP -> Optional.of(NamedKey.UP);
            case DOWN -> Optional.of(NamedKey.DOWN);
            case LEFT -> Optional.of(NamedKey.LEFT);
            case RIGHT -> Optional.of(NamedKey.RIGHT);
            case HOME -> Optional.of(NamedKey.HOME);
            case END -> Optional.of(NamedKey.END);
            case PAGE_UP -> Optional.of(NamedKey.PAGE_UP);
            case PAGE_DOWN -> Optional.of(NamedKey.PAGE_DOWN);
            case F1 -> Optional.of(NamedKey.F1);
            case F2 -> Optional.of(NamedKey.F2);
            case F3 -> Optional.of(NamedKey.F3);
            case F4 -> Optional.of(NamedKey.F4);
            case F5 -> Optional.of(NamedKey.F5);
            case F6 -> Optional.of(NamedKey.F6);
            case F7 -> Optional.of(NamedKey.F7);
            case F8 -> Optional.of(NamedKey.F8);
            case F9 -> Optional.of(NamedKey.F9);
            case F10 -> Optional.of(NamedKey.F10);
            case F11 -> Optional.of(NamedKey.F11);
            case F12 -> Optional.of(NamedKey.F12);
            case CHAR, UNKNOWN -> Optional.empty();
        };
    }

    private static Modifiers modifiers(KeyModifiers held) {
        List<Modifier> modifiers = new ArrayList<>();
        if (held.ctrl()) {
            modifiers.add(Modifier.CTRL);
        }
        if (held.alt()) {
            modifiers.add(Modifier.ALT);
        }
        if (held.shift()) {
            modifiers.add(Modifier.SHIFT);
        }
        return new Modifiers(modifiers);
    }

    private static MouseAction action(MouseEvent event) {
        return switch (event.kind()) {
            case PRESS -> MouseAction.PRESS;
            case RELEASE -> MouseAction.RELEASE;
            case DRAG -> MouseAction.DRAG;
            case MOVE -> MouseAction.MOVE;
            case SCROLL_UP -> MouseAction.SCROLL_UP;
            case SCROLL_DOWN -> MouseAction.SCROLL_DOWN;
            case SCROLL_LEFT -> MouseAction.SCROLL_LEFT;
            case SCROLL_RIGHT -> MouseAction.SCROLL_RIGHT;
        };
    }

    private static PointerButton button(MouseButton button) {
        return switch (button) {
            case LEFT -> PointerButton.LEFT;
            case MIDDLE -> PointerButton.MIDDLE;
            case RIGHT -> PointerButton.RIGHT;
            case NONE -> PointerButton.NONE;
        };
    }
}

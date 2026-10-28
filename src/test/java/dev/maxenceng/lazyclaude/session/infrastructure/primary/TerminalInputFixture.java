package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.PointerButton;
import dev.maxenceng.lazyclaude.session.published.PressedKey;
import dev.maxenceng.lazyclaude.session.published.TypedCharacter;

public final class TerminalInputFixture {

    private TerminalInputFixture() {}

    public static TerminalKey typedA() {
        return typed('a', Modifiers.none());
    }

    public static TerminalKey typed(int codePoint, Modifiers modifiers) {
        return TerminalKey.builder()
                .input(new TypedCharacter(codePoint))
                .modifiers(modifiers)
                .build();
    }

    public static TerminalKey pressed(NamedKey key) {
        return pressed(key, Modifiers.none());
    }

    public static TerminalKey pressed(NamedKey key, Modifiers modifiers) {
        return TerminalKey.builder()
                .input(new PressedKey(key))
                .modifiers(modifiers)
                .build();
    }

    public static PastedText pasted(String text) {
        return new PastedText(text);
    }

    public static TerminalMouse mouse(MouseAction action, PointerButton button, int column, int row) {
        return TerminalMouse.builder()
                .action(action)
                .button(button)
                .column(new CellColumn(column))
                .row(new CellRow(row))
                .build();
    }

    public static TerminalMouse wheelUpAt(int column, int row) {
        return mouse(MouseAction.SCROLL_UP, PointerButton.NONE, column, row);
    }

    public static TerminalMouse leftPressAt(int column, int row) {
        return mouse(MouseAction.PRESS, PointerButton.LEFT, column, row);
    }
}

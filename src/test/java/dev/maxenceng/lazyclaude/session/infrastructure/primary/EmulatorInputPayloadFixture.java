package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.PointerButton;
import dev.maxenceng.lazyclaude.session.published.PressedKey;
import dev.maxenceng.lazyclaude.session.published.TypedCharacter;

public final class EmulatorInputPayloadFixture {

    private EmulatorInputPayloadFixture() {}

    public static EmulatorKeyPayload typedA() {
        return typed('a', Modifiers.none());
    }

    public static EmulatorKeyPayload typed(int codePoint, Modifiers modifiers) {
        return EmulatorKeyPayload.builder()
                .input(new TypedCharacter(codePoint))
                .modifiers(modifiers)
                .build();
    }

    public static EmulatorKeyPayload pressed(NamedKey key) {
        return pressed(key, Modifiers.none());
    }

    public static EmulatorKeyPayload pressed(NamedKey key, Modifiers modifiers) {
        return EmulatorKeyPayload.builder()
                .input(new PressedKey(key))
                .modifiers(modifiers)
                .build();
    }

    public static EmulatorPastePayload pasted(String text) {
        return new EmulatorPastePayload(text);
    }

    public static EmulatorMousePayload mouse(MouseAction action, PointerButton button, int column, int row) {
        return EmulatorMousePayload.builder()
                .action(action)
                .button(button)
                .column(new CellColumn(column))
                .row(new CellRow(row))
                .build();
    }

    public static EmulatorMousePayload wheelUpAt(int column, int row) {
        return mouse(MouseAction.SCROLL_UP, PointerButton.NONE, column, row);
    }

    public static EmulatorMousePayload leftPressAt(int column, int row) {
        return mouse(MouseAction.PRESS, PointerButton.LEFT, column, row);
    }
}

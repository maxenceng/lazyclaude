package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.PointerButton;

/** A mouse event over the pane, at the cell it happened on, counted from the pane's top left. */
public record EmulatorMousePayload(
        MouseAction action, PointerButton button, CellColumn column, CellRow row, Modifiers modifiers)
        implements EmulatorInputPayload {

    public EmulatorMousePayload {
        Assert.notNull("mouse action", action);
        Assert.notNull("mouse button", button);
        Assert.notNull("column", column);
        Assert.notNull("row", row);
        Assert.notNull("modifiers", modifiers);
    }

    public static EmulatorMousePayloadBuilder builder() {
        return new EmulatorMousePayloadBuilder();
    }

    public static final class EmulatorMousePayloadBuilder {
        private MouseAction action;
        private PointerButton button = PointerButton.NONE;
        private CellColumn column;
        private CellRow row;
        private Modifiers modifiers = Modifiers.none();

        private EmulatorMousePayloadBuilder() {}

        public EmulatorMousePayloadBuilder action(MouseAction action) {
            this.action = action;
            return this;
        }

        public EmulatorMousePayloadBuilder button(PointerButton button) {
            this.button = button;
            return this;
        }

        public EmulatorMousePayloadBuilder column(CellColumn column) {
            this.column = column;
            return this;
        }

        public EmulatorMousePayloadBuilder row(CellRow row) {
            this.row = row;
            return this;
        }

        public EmulatorMousePayloadBuilder modifiers(Modifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public EmulatorMousePayload build() {
            return new EmulatorMousePayload(action, button, column, row, modifiers);
        }
    }
}

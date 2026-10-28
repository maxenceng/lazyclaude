package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.PointerButton;

/** A mouse event over the pane, at the cell it happened on, counted from the pane's top left. */
public record TerminalMouse(
        MouseAction action, PointerButton button, CellColumn column, CellRow row, Modifiers modifiers)
        implements TerminalInput {

    public TerminalMouse {
        Assert.notNull("mouse action", action);
        Assert.notNull("mouse button", button);
        Assert.notNull("column", column);
        Assert.notNull("row", row);
        Assert.notNull("modifiers", modifiers);
    }

    public static TerminalMouseBuilder builder() {
        return new TerminalMouseBuilder();
    }

    public static final class TerminalMouseBuilder {
        private MouseAction action;
        private PointerButton button = PointerButton.NONE;
        private CellColumn column;
        private CellRow row;
        private Modifiers modifiers = Modifiers.none();

        private TerminalMouseBuilder() {}

        public TerminalMouseBuilder action(MouseAction action) {
            this.action = action;
            return this;
        }

        public TerminalMouseBuilder button(PointerButton button) {
            this.button = button;
            return this;
        }

        public TerminalMouseBuilder column(CellColumn column) {
            this.column = column;
            return this;
        }

        public TerminalMouseBuilder row(CellRow row) {
            this.row = row;
            return this;
        }

        public TerminalMouseBuilder modifiers(Modifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public TerminalMouse build() {
            return new TerminalMouse(action, button, column, row, modifiers);
        }
    }
}

package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The session's cursor is on a cell of the pane. */
public record CursorAt(CellColumn column, CellRow row) implements ScreenCursor {

    public CursorAt {
        Assert.notNull("column", column);
        Assert.notNull("row", row);
    }

    public static CursorAtBuilder builder() {
        return new CursorAtBuilder();
    }

    public static final class CursorAtBuilder {
        private CellColumn column;
        private CellRow row;

        private CursorAtBuilder() {}

        public CursorAtBuilder column(CellColumn column) {
            this.column = column;
            return this;
        }

        public CursorAtBuilder row(CellRow row) {
            this.row = row;
            return this;
        }

        public CursorAt build() {
            return new CursorAt(column, row);
        }
    }
}

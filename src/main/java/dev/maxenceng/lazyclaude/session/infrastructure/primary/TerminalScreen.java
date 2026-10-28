package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.NoCursor;
import dev.maxenceng.lazyclaude.session.published.ScreenCursor;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;

/** What a hosted session's terminal shows right now, cell for cell: its rows of styled runs and its cursor. */
public record TerminalScreen(List<ScreenRow> rows, ScreenCursor cursor) {

    public TerminalScreen {
        Assert.notNull("rows", rows);
        Assert.notNull("cursor", cursor);
    }

    /** What a session shows before it has drawn anything. */
    public static TerminalScreen blank() {
        return builder().build();
    }

    public static TerminalScreenBuilder builder() {
        return new TerminalScreenBuilder();
    }

    public static final class TerminalScreenBuilder {
        private List<ScreenRow> rows = List.of();
        private ScreenCursor cursor = new NoCursor();

        private TerminalScreenBuilder() {}

        public TerminalScreenBuilder rows(List<ScreenRow> rows) {
            this.rows = rows;
            return this;
        }

        public TerminalScreenBuilder cursor(ScreenCursor cursor) {
            this.cursor = cursor;
            return this;
        }

        public TerminalScreen build() {
            return new TerminalScreen(rows, cursor);
        }
    }
}

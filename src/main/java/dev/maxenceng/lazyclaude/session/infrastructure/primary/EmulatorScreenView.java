package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.NoCursor;
import dev.maxenceng.lazyclaude.session.published.ScreenCursor;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;

/** What a hosted session's emulator shows right now, cell for cell: its rows of styled runs and its cursor. */
public record EmulatorScreenView(List<ScreenRow> rows, ScreenCursor cursor) {

    public EmulatorScreenView {
        Assert.notNull("rows", rows);
        Assert.notNull("cursor", cursor);
    }

    public static EmulatorScreenViewBuilder builder() {
        return new EmulatorScreenViewBuilder();
    }

    public static final class EmulatorScreenViewBuilder {
        private List<ScreenRow> rows = List.of();
        private ScreenCursor cursor = new NoCursor();

        private EmulatorScreenViewBuilder() {}

        public EmulatorScreenViewBuilder rows(List<ScreenRow> rows) {
            this.rows = rows;
            return this;
        }

        public EmulatorScreenViewBuilder cursor(ScreenCursor cursor) {
            this.cursor = cursor;
            return this;
        }

        public EmulatorScreenView build() {
            return new EmulatorScreenView(rows, cursor);
        }
    }
}

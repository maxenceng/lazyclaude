package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;

import dev.tamboui.layout.Rect;

final class DiffDrawnFixture {

    private DiffDrawnFixture() {}

    static DiffDrawn headerThenTwoLinesThenAThread() {
        return new DiffDrawn(
                new Rect(10, 5, 40, 4), List.of(Optional.empty(), Optional.of(7), Optional.of(8), Optional.empty()));
    }

    static DiffDrawn headersOnly() {
        return new DiffDrawn(new Rect(10, 5, 40, 2), List.of(Optional.empty(), Optional.empty()));
    }
}

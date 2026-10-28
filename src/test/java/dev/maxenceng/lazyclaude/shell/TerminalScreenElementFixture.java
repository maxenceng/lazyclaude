package dev.maxenceng.lazyclaude.shell;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastedText;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TerminalScreen;

final class TerminalScreenElementFixture {

    private TerminalScreenElementFixture() {}

    static TerminalScreenElement elementOf(TerminalScreen screen, boolean focused, Consumer<PastedText> onPaste) {
        return new TerminalScreenElement(screen, focused, onPaste);
    }
}

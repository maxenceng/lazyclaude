package dev.maxenceng.lazyclaude.shell;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorPastePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorScreenView;

final class EmulatorScreenElementFixture {

    private EmulatorScreenElementFixture() {}

    static EmulatorScreenElement elementOf(EmulatorScreenView screen, boolean focused, Consumer<EmulatorPastePayload> onPaste) {
        return new EmulatorScreenElement(screen, focused, onPaste);
    }
}

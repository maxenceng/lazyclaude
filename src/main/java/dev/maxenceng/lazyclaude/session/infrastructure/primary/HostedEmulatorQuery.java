package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What the TUI reads of a hosted session's emulator: the screen each session's output drew, whether it follows the pointer, and that one of them drew again. */
public final class HostedEmulatorQuery {

    private final HostedEmulators emulators;

    public HostedEmulatorQuery(HostedEmulators emulators) {
        this.emulators = emulators;
    }

    /** What a hosted session shows right now, blank for one that is gone. */
    public EmulatorScreenView screenOf(SessionId id) {
        return emulators.of(id).map(HostedEmulator::screen).orElseGet(() -> EmulatorScreenView.builder().build());
    }

    /** Whether a hosted session asked for every pointer move (mode 1003), so a move over its pane is worth sending; not for one that is gone. */
    public boolean followsPointer(SessionId id) {
        return emulators.of(id).map(HostedEmulator::followsPointer).orElse(false);
    }

    /** Tells {@code listener} whenever a hosted session drew something, on a thread of the emulator's own. */
    public void onScreenChange(Runnable listener) {
        emulators.onScreenChange(listener);
    }
}

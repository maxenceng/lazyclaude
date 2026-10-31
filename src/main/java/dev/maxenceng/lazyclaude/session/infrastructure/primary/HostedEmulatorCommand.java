package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;

/**
 * What the TUI asks of a hosted session's emulator: what is typed, pasted or clicked at it is encoded by the modes its
 * {@code claude} switched on and written to its pty, and a new pane size reaches the emulator before the pty.
 */
public final class HostedEmulatorCommand {

    private final SessionApplicationService sessions;
    private final HostedEmulators emulators;

    public HostedEmulatorCommand(SessionApplicationService sessions, HostedEmulators emulators) {
        this.sessions = sessions;
        this.emulators = emulators;
    }

    /**
     * Runs an emulator for every hosted session launched from now on. Only an output that draws the sessions asks for it: the
     * sidecar hands the pty's bytes to its frontend instead, and an emulator nobody reads is a terminal parsed for nothing.
     */
    public void emulate() {
        emulators.listen();
    }

    /** Encodes the key, paste or mouse event for the session's emulator and writes it to its pty; nothing for a session that is gone. */
    public void send(HostedInputPayload payload) {
        emulators.of(payload.session()).ifPresent(emulator -> write(payload, emulator.encode(payload.input())));
    }

    private void write(HostedInputPayload payload, PtyBytes bytes) {
        if (!bytes.isEmpty()) {
            sessions.write(payload.session(), bytes);
        }
    }

    /** Gives the session's emulator a new size, and only then its pty, so what {@code claude} redraws lands in a screen of that size. */
    public void resize(ResizePayload payload) {
        emulators.of(payload.session()).ifPresent(emulator -> {
            emulator.resize(payload.size());
            sessions.resize(payload.session(), payload.size());
        });
    }
}

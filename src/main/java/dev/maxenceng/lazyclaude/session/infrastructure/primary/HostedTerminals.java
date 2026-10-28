package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * What the TUI asks of a hosted session's terminal: each session's output is emulated here, so what it shows is read from here, and
 * what is typed, pasted or clicked at it is encoded here by the modes its {@code claude} switched on and written to its pty.
 */
public final class HostedTerminals {

    private final SessionApplicationService sessions;
    private final HostedEmulators emulators;

    public HostedTerminals(SessionApplicationService sessions) {
        this.sessions = sessions;
        this.emulators = new HostedEmulators(sessions);
        sessions.onPty(emulators);
    }

    /** Encodes the key, paste or mouse event for the session's terminal and writes it to its pty; nothing for a session that is gone. */
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

    /** What a hosted session shows right now, blank for one that is gone. */
    public TerminalScreen screenOf(SessionId id) {
        return emulators.of(id).map(HostedEmulator::screen).orElseGet(TerminalScreen::blank);
    }

    /** Tells {@code listener} whenever a hosted session drew something, on a thread of the emulator's own. */
    public void onScreenChange(Runnable listener) {
        emulators.onScreenChange(listener);
    }
}

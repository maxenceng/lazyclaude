package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.io.IOException;
import java.util.function.Consumer;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.TtyBasedArrayDataStream;
import com.jediterm.terminal.emulator.JediEmulator;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * One hosted session's terminal in the TUI: an emulator fed the session's pty bytes on a thread of its own, the screen it draws,
 * the encoding of what is typed at it by the modes the child switched on, and the replies to the child's queries.
 */
final class HostedEmulator {

    private static final String EMULATOR_NAME = "emulator-%s";

    private final String name;
    private final PtyBytesQueue fed = new PtyBytesQueue();
    private final ScreenDisplay display = new ScreenDisplay();
    private final TerminalTextBuffer buffer;
    private final HostedTerminal terminal;
    private final JediEmulator emulator;

    /** {@code pty} is where the replies to the child's queries go. */
    HostedEmulator(SessionId id, PaneSize size, Consumer<PtyBytes> pty) {
        this.name = EMULATOR_NAME.formatted(id.value());
        StyleState style = new StyleState();
        this.buffer = new TerminalTextBuffer(size.width().cells(), size.height().cells(), style);
        this.terminal = new HostedTerminal(display, buffer, style);
        this.terminal.setTerminalOutput(new TerminalReplies(pty));
        this.emulator = new JediEmulator(new TtyBasedArrayDataStream(new EmulatorConnector(fed, pty)), terminal);
    }

    /** Starts emulating what is fed, until {@link #end}. */
    void start(Runnable onScreenChange) {
        buffer.addModelListener(onScreenChange::run);
        Thread.ofPlatform().daemon().name(name).start(this::emulate);
    }

    // deferred: an emulator that throws stops drawing and leaves the session's last screen up, its output dropped, since nothing ends the session from here — close it when a frozen pane is seen.
    private void emulate() {
        try {
            while (emulator.hasNext()) {
                emulator.next();
            }
        } catch (IOException closed) {
            // the stream closing under the emulator is how the end of the session shows up here
        } finally {
            fed.stop();
        }
    }

    /** Hands the emulator what the child wrote; it is read on the emulator's own thread. */
    void feed(PtyBytes bytes) {
        fed.offer(bytes);
    }

    /** What the child's modes make of {@code input}. */
    PtyBytes encode(TerminalInput input) {
        return new PtyBytes(
                switch (input) {
                    case TerminalKey key -> KeyBytes.of(key, terminal.applicationCursor());
                    case PastedText paste -> PasteBytes.of(paste, display.bracketedPaste());
                    case TerminalMouse mouse -> MouseBytes.of(mouse, display.mouseMode());
                });
    }

    void resize(PaneSize size) {
        terminal.resize(new TermSize(size.width().cells(), size.height().cells()), RequestOrigin.User);
    }

    TerminalScreen screen() {
        return ScreenCopy.of(
                buffer, new CellColumn(terminal.getX()), new CellRow(terminal.getY() - 1), display.cursorVisible());
    }

    void end() {
        fed.close();
    }
}

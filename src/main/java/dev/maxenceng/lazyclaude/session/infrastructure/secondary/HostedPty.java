package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.TerminalOutputStream;
import com.jediterm.terminal.TtyBasedArrayDataStream;
import com.jediterm.terminal.emulator.JediEmulator;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostUnavailableException;
import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.PastedText;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;
import dev.maxenceng.lazyclaude.session.published.TerminalKey;
import dev.maxenceng.lazyclaude.session.published.TerminalMouse;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;

/** One {@code claude} on a pty: the process, the emulator that reads its output, and the encoding of what is sent to it. */
final class HostedPty {

    private static final String START_FAILURE = "claude could not be started: %s";
    private static final long GRACE_SECONDS = 1;

    private final PtyProcess process;
    private final PtyConnector connector;
    private final ScreenDisplay display;
    private final HostedTerminal terminal;
    private final TerminalTextBuffer buffer;
    private final JediEmulator emulator;

    private HostedPty(PtyProcess process, PaneSize size) {
        this.process = process;
        this.connector = new PtyConnector(process);
        this.display = new ScreenDisplay();
        StyleState style = new StyleState();
        this.buffer = new TerminalTextBuffer(size.width().cells(), size.height().cells(), style);
        this.terminal = new HostedTerminal(display, buffer, style);
        this.terminal.setTerminalOutput(replies());
        this.emulator = new JediEmulator(new TtyBasedArrayDataStream(connector), terminal);
    }

    /**
     * Starts {@code argv} on a pty of {@code size} in {@code directory}.
     *
     * @throws HostUnavailableException when the process cannot be started
     */
    static HostedPty spawn(List<String> argv, Map<String, String> environment, Path directory, PaneSize size) {
        try {
            PtyProcess process = new PtyProcessBuilder(argv.toArray(String[]::new))
                    .setEnvironment(environment)
                    .setDirectory(directory.toString())
                    .setInitialColumns(size.width().cells())
                    .setInitialRows(size.height().cells())
                    .start();
            return new HostedPty(process, size);
        } catch (IOException | RuntimeException failure) {
            throw new HostUnavailableException(START_FAILURE.formatted(failure.getMessage()), failure);
        }
    }

    ProcessId pid() {
        return new ProcessId(process.pid());
    }

    /** Starts reading the child's output on a thread of its own, which ends by telling {@code onExit} the exit code. */
    void listen(Runnable onScreenChange, Consumer<ExitCode> onExit) {
        buffer.addModelListener(onScreenChange::run);
        Thread.ofPlatform().daemon().name("pty-" + process.pid()).start(() -> read(onExit));
    }

    private void read(Consumer<ExitCode> onExit) {
        try {
            while (emulator.hasNext()) {
                emulator.next();
            }
        } catch (IOException closed) {
            // the pty closing under the reader is how a child's end shows up, so the exit code below is the news
        }
        onExit.accept(new ExitCode(exitCode()));
    }

    private int exitCode() {
        try {
            return process.waitFor();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return process.isAlive() ? -1 : process.exitValue();
        }
    }

    void send(TerminalInput input) {
        byte[] bytes = switch (input) {
            case TerminalKey key -> KeyBytes.of(key, terminal.applicationCursor());
            case PastedText paste -> PasteBytes.of(paste, display.bracketedPaste());
            case TerminalMouse mouse -> MouseBytes.of(mouse, display.mouseMode());
        };
        write(bytes);
    }

    void resize(PaneSize size) {
        process.setWinSize(new WinSize(size.width().cells(), size.height().cells()));
        terminal.resize(new TermSize(size.width().cells(), size.height().cells()), RequestOrigin.User);
    }

    TerminalScreen screen() {
        return ScreenCopy.of(
                buffer, new CellColumn(terminal.getX()), new CellRow(terminal.getY() - 1), display.cursorVisible());
    }

    /** Asks the child to end, and makes sure of it when it does not within a moment. */
    void end() {
        process.destroy();
        try {
            if (!process.waitFor(GRACE_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private void write(byte[] bytes) {
        if (bytes.length == 0) {
            return;
        }
        try {
            connector.write(bytes);
        } catch (IOException gone) {
            // the child has exited and the reader is about to say so; what was typed at it has nowhere to go
        }
    }

    private TerminalOutputStream replies() {
        return new TerminalReplies(this::write);
    }
}

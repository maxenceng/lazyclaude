package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.TtyConnector;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;

/** What the emulator reads its input from and writes to: the pty bytes fed to it, and replies that go back to the pty. */
final class EmulatorConnector implements TtyConnector {

    private static final String NAME = "claude";

    private final PtyBytesQueue fed;
    private final Reader output;
    private final Consumer<PtyBytes> pty;

    EmulatorConnector(PtyBytesQueue fed, Consumer<PtyBytes> pty) {
        this.fed = fed;
        this.output = new InputStreamReader(fed, StandardCharsets.UTF_8);
        this.pty = pty;
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        return output.read(buf, offset, length);
    }

    @Override
    public void write(byte[] bytes) {
        pty.accept(new PtyBytes(bytes));
    }

    @Override
    public void write(String string) {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void resize(TermSize termSize) {}

    @Override
    public int waitFor() {
        return 0;
    }

    @Override
    public boolean ready() throws IOException {
        return output.ready();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void close() {
        fed.close();
    }
}

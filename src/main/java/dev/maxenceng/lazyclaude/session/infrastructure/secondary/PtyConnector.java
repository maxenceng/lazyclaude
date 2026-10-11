package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;

/** The pty of one child, as the emulator reads and writes it. */
final class PtyConnector implements TtyConnector {

    private static final String NAME = "claude";

    private final PtyProcess process;
    private final Reader output;
    private final OutputStream input;

    PtyConnector(PtyProcess process) {
        this.process = process;
        this.output = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
        this.input = process.getOutputStream();
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        return output.read(buf, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        input.write(bytes);
        input.flush();
    }

    @Override
    public void write(String string) throws IOException {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isConnected() {
        return process.isAlive();
    }

    @Override
    public void resize(TermSize termSize) {
        process.setWinSize(new WinSize(termSize.getColumns(), termSize.getRows()));
    }

    @Override
    public int waitFor() throws InterruptedException {
        return process.waitFor();
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
        process.destroy();
    }
}

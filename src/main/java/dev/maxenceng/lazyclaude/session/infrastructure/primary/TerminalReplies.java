package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.jediterm.terminal.TerminalOutputStream;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;

/** What the emulator answers to the child's queries (device attributes, cursor position), written back to its pty. */
final class TerminalReplies implements TerminalOutputStream {

    private final Consumer<PtyBytes> pty;

    TerminalReplies(Consumer<PtyBytes> pty) {
        this.pty = pty;
    }

    @Override
    public void sendBytes(byte[] response, boolean userInput) {
        pty.accept(new PtyBytes(response));
    }

    @Override
    public void sendString(String string, boolean userInput) {
        pty.accept(new PtyBytes(string.getBytes(StandardCharsets.UTF_8)));
    }
}

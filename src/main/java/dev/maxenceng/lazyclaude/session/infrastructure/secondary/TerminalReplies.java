package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.jediterm.terminal.TerminalOutputStream;

/** What the emulator answers to the child's queries (device attributes, cursor position), written back to its pty. */
final class TerminalReplies implements TerminalOutputStream {

    private final Consumer<byte[]> pty;

    TerminalReplies(Consumer<byte[]> pty) {
        this.pty = pty;
    }

    @Override
    public void sendBytes(byte[] response, boolean userInput) {
        pty.accept(response);
    }

    @Override
    public void sendString(String string, boolean userInput) {
        pty.accept(string.getBytes(StandardCharsets.UTF_8));
    }
}

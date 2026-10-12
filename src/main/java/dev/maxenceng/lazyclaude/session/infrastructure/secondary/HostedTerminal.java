package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import com.jediterm.terminal.TerminalDisplay;
import com.jediterm.terminal.model.JediTerminal;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;

/** The emulator's terminal, which also remembers whether the child asked for application cursor keys, since the keys sent to it depend on it. */
final class HostedTerminal extends JediTerminal {

    private volatile boolean applicationCursor;

    HostedTerminal(TerminalDisplay display, TerminalTextBuffer buffer, StyleState style) {
        super(display, buffer, style);
    }

    boolean applicationCursor() {
        return applicationCursor;
    }

    @Override
    public void setApplicationArrowKeys(boolean enabled) {
        this.applicationCursor = enabled;
        super.setApplicationArrowKeys(enabled);
    }
}

package dev.maxenceng.lazyclaude.shell;

import java.io.IOException;

/** No terminal backend could be opened, so the screen cannot start. */
final class TerminalUnavailableException extends IllegalStateException {

    TerminalUnavailableException(IOException cause) {
        super("cannot open the terminal: %s".formatted(cause.getMessage()), cause);
    }
}

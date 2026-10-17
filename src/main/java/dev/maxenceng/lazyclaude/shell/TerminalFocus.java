package dev.maxenceng.lazyclaude.shell;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whether the terminal window has focus. It counts as focused until a focus-out arrives, so a terminal that never reports focus
 * never silences a notification by mistake. Callable from any thread.
 */
final class TerminalFocus {

    private final AtomicBoolean focused = new AtomicBoolean(true);
    private final Runnable changed;

    /** {@code changed} runs, on the thread that reported it, whenever focus really moved. */
    TerminalFocus(Runnable changed) {
        this.changed = changed;
    }

    boolean focused() {
        return focused.get();
    }

    void gained() {
        if (!focused.getAndSet(true)) {
            changed.run();
        }
    }

    void lost() {
        if (focused.getAndSet(false)) {
            changed.run();
        }
    }
}

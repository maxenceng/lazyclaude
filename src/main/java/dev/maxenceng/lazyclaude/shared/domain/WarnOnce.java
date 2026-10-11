package dev.maxenceng.lazyclaude.shared.domain;

import java.util.concurrent.atomic.AtomicBoolean;

/** One degraded mode's turn to be named: the first report raises its warning, every later one is already said. */
public final class WarnOnce {

    private final AtomicBoolean said = new AtomicBoolean();

    /** Raises {@code warning} on the first call only; callable from any thread. */
    public void raise(WarningPort warnings, Warning warning) {
        if (said.compareAndSet(false, true)) {
            warnings.raise(warning);
        }
    }
}

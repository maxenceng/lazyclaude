package dev.maxenceng.lazyclaude.shared.domain;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One degraded mode's turn to be named: the first report raises its warning, every later one is already said, until the mode ends and
 * the warning is withdrawn, so the next time it starts it is named again.
 */
public final class WarnOnce {

    private final AtomicReference<Optional<Warning>> said = new AtomicReference<>(Optional.empty());

    /** Raises {@code warning} when none of this mode is raised; callable from any thread. */
    public void raise(WarningPort warnings, Warning warning) {
        if (said.compareAndSet(Optional.empty(), Optional.of(warning))) {
            warnings.raise(warning);
        }
    }

    /** Takes back the warning it raised, if it is still raised; callable from any thread. */
    public void withdraw(WarningPort warnings) {
        said.getAndSet(Optional.empty()).ifPresent(warnings::withdraw);
    }
}

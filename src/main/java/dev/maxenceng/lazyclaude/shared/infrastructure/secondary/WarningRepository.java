package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Warnings;

/**
 * Keeps every warning raised, in memory, until the process ends. A subscriber is handed the held ones at
 * once, which is how a warning raised before the screen exists reaches its first frame, and the lot again
 * after each one raised later. Raising and subscribing take one lock, so a warning is neither missed nor
 * handed twice by a subscriber that arrives as one is raised.
 */
public final class WarningRepository implements WarningPort, WarningWatchPort {

    private final Object lock = new Object();
    private final List<Warning> raised = new ArrayList<>();
    private final List<Consumer<Warnings>> listeners = new ArrayList<>();

    @Override
    public void raise(Warning warning) {
        synchronized (lock) {
            raised.add(warning);
            Warnings held = new Warnings(List.copyOf(raised));
            listeners.forEach(listener -> listener.accept(held));
        }
    }

    @Override
    public void subscribe(Consumer<Warnings> listener) {
        synchronized (lock) {
            listeners.add(listener);
            if (!raised.isEmpty()) {
                listener.accept(new Warnings(List.copyOf(raised)));
            }
        }
    }
}

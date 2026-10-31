package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Warnings;

/**
 * Keeps every warning raised, in memory, until the process ends or the warning is withdrawn. A subscriber is handed the held ones at
 * once, which is how a warning raised before the screen exists reaches its first frame, and the lot again
 * after each one raised later. Raising and subscribing take one lock, so a warning is neither missed nor
 * handed twice by a subscriber that arrives as one is raised.
 */
public final class WarningRepository implements WarningPort, WarningWatchPort {

    private final Object lock = new Object();
    private List<Warning> raised = List.of();
    private final List<Consumer<Warnings>> listeners = new ArrayList<>();
    private final List<Runnable> noneHeld = new ArrayList<>();

    @Override
    public void raise(Warning warning) {
        synchronized (lock) {
            raised = Stream.concat(raised.stream(), Stream.of(warning)).toList();
            Warnings held = new Warnings(raised);
            listeners.forEach(listener -> listener.accept(held));
        }
    }

    @Override
    public void withdraw(Warning warning) {
        synchronized (lock) {
            int at = raised.indexOf(warning);
            if (at < 0) {
                return;
            }
            raised = Stream.concat(raised.subList(0, at).stream(), raised.subList(at + 1, raised.size()).stream()).toList();
            if (raised.isEmpty()) {
                noneHeld.forEach(Runnable::run);
                return;
            }
            Warnings held = new Warnings(raised);
            listeners.forEach(listener -> listener.accept(held));
        }
    }

    @Override
    public void subscribe(Consumer<Warnings> listener) {
        synchronized (lock) {
            listeners.add(listener);
            if (!raised.isEmpty()) {
                listener.accept(new Warnings(raised));
            }
        }
    }

    @Override
    public Optional<Warnings> held() {
        synchronized (lock) {
            return raised.isEmpty() ? Optional.empty() : Optional.of(new Warnings(raised));
        }
    }

    @Override
    public void onNoneHeld(Runnable listener) {
        synchronized (lock) {
            noneHeld.add(listener);
            if (raised.isEmpty()) {
                listener.run();
            }
        }
    }
}

package dev.maxenceng.lazyclaude.shared.application;

import java.util.Optional;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.shared.domain.WarningManager;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Warnings;

/** Keeps a listener handed the warnings raised, those raised before it asked included, and raises one for a thread that stopped. */
public final class WarningApplicationService {

    private final WarningManager manager;

    public WarningApplicationService(WarningWatchPort watch, WarningPort warnings) {
        this.manager = new WarningManager(watch, warnings);
    }

    public void subscribe(Consumer<Warnings> listener) {
        manager.subscribe(listener);
    }

    public Optional<Warnings> held() {
        return manager.held();
    }

    public void onNoneHeld(Runnable listener) {
        manager.onNoneHeld(listener);
    }

    public void threadStopped(String thread, String failure) {
        manager.threadStopped(thread, failure);
    }
}

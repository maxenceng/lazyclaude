package dev.maxenceng.lazyclaude.shared.application;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.shared.domain.WarningManager;
import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Warnings;

/** Keeps a listener handed the warnings raised, those raised before it asked included. */
public final class WarningApplicationService {

    private final WarningManager manager;

    public WarningApplicationService(WarningWatchPort watch) {
        this.manager = new WarningManager(watch);
    }

    public void subscribe(Consumer<Warnings> listener) {
        manager.subscribe(listener);
    }
}

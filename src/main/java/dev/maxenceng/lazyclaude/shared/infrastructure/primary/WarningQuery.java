package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;

/** What the shell asks of the shared kernel: a view of the warnings raised, again after each new one. It only reads. */
public final class WarningQuery {

    private final WarningApplicationService warnings;

    public WarningQuery(WarningApplicationService warnings) {
        this.warnings = warnings;
    }

    /** Hands {@code listener} a view at once if any warning is held, and after each one raised later, on the thread that raised it. */
    public void subscribe(Consumer<WarningView> listener) {
        warnings.subscribe(held -> listener.accept(WarningView.from(held)));
    }
}

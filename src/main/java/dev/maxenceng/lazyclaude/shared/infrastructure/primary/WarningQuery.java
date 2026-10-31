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

    /** The warnings held now as the status line shows them, or that none is held. */
    public WarningsView held() {
        return warnings.held().<WarningsView>map(WarningView::from).orElseGet(NoWarningView::new);
    }

    /** Runs {@code listener} at once if no warning is held, and each time the last one held is withdrawn: the status line has nothing to show. */
    public void onNoneHeld(Runnable listener) {
        warnings.onNoneHeld(listener);
    }

    /**
     * Hands {@code listener} what the status line shows at once, and again after each warning raised or the last one withdrawn, on the
     * thread that did it: a frontend that never learns of a withdrawal is told that none is held.
     */
    public void watch(Consumer<WarningsView> listener) {
        subscribe(listener::accept);
        onNoneHeld(() -> listener.accept(new NoWarningView()));
    }
}

package dev.maxenceng.lazyclaude.shared.domain;

import java.util.function.Consumer;

/**
 * Passes the warnings raised so far, and each one raised after, to whoever shows them, tells them when none is left held, and names a
 * thread that stopped on a failure nobody caught.
 */
public record WarningManager(WarningWatchPort watch, WarningPort warnings) {

    private static final String STOPPED_WARNING = "%s stopped (%s); what it kept current no longer updates.";

    public void subscribe(Consumer<Warnings> listener) {
        watch.subscribe(listener);
    }

    public void onNoneHeld(Runnable listener) {
        watch.onNoneHeld(listener);
    }

    public void threadStopped(String thread, String failure) {
        warnings.raise(new Warning(STOPPED_WARNING.formatted(thread, failure)));
    }
}

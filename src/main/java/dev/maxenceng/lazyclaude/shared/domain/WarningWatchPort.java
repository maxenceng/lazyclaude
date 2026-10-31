package dev.maxenceng.lazyclaude.shared.domain;

import java.util.Optional;
import java.util.function.Consumer;

/** Tells the domain which warnings have been raised, those raised before it asked included. */
public interface WarningWatchPort {

    /**
     * Hands {@code listener} every warning held so far at once, if any, and again after each one raised from now on,
     * on the thread that raised it, for as long as the process lives.
     */
    void subscribe(Consumer<Warnings> listener);

    /** Every warning held now, none when none is. */
    Optional<Warnings> held();

    /**
     * Runs {@code listener} at once if no warning is held, and each time the last one held is withdrawn, on the thread that withdrew it,
     * for as long as the process lives.
     */
    void onNoneHeld(Runnable listener);
}

package dev.maxenceng.lazyclaude.shared.domain;

/** Where a degraded mode is named, so the screen can say so instead of the process printing over it. */
public interface WarningPort {

    /** Keeps {@code warning} until lazyclaude quits; callable from any thread. */
    void raise(Warning warning);
}

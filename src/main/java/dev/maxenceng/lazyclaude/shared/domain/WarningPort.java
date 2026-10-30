package dev.maxenceng.lazyclaude.shared.domain;

/** Where a degraded mode is named, so the screen can say so instead of the process printing over it. */
public interface WarningPort {

    /** Keeps {@code warning} until it is withdrawn or lazyclaude quits; callable from any thread. */
    void raise(Warning warning);

    /** Takes back one raised {@code warning}, equal to it, whose cause is gone; callable from any thread. */
    void withdraw(Warning warning);
}

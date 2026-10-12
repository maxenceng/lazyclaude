package dev.maxenceng.lazyclaude.session.domain;

/** Told when a hosted session opens or closes, on whichever thread caused it. */
@FunctionalInterface
public interface OpenSessionsListener {

    void changed();
}

package dev.maxenceng.lazyclaude.shared.domain;

import java.util.function.Consumer;

/** Passes the warnings raised so far, and each one raised after, to whoever shows them. */
public record WarningManager(WarningWatchPort watch) {

    public void subscribe(Consumer<Warnings> listener) {
        watch.subscribe(listener);
    }
}

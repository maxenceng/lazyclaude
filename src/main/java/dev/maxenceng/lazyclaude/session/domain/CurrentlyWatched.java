package dev.maxenceng.lazyclaude.session.domain;

import java.util.concurrent.atomic.AtomicReference;

/** Which session the user is looking at now; none until the shell says. */
public final class CurrentlyWatched {

    private final AtomicReference<Watched> now = new AtomicReference<>(new NoWatchedSession());

    public Watched get() {
        return now.get();
    }

    public void watch(Watched next) {
        now.set(next);
    }
}

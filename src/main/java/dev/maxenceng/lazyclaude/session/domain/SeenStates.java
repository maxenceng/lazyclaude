package dev.maxenceng.lazyclaude.session.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionState;

/** How the machine's live sessions stood at the last read; nothing before the first one. */
public final class SeenStates {

    private final AtomicReference<Optional<Map<SessionId, SessionState>>> last = new AtomicReference<>(Optional.empty());

    /** Remembers {@code next} and answers what was remembered before it: empty on the first read. */
    public Optional<Map<SessionId, SessionState>> exchange(Map<SessionId, SessionState> next) {
        return last.getAndSet(Optional.of(next));
    }
}

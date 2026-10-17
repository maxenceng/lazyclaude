package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.NoWatchedSession;
import dev.maxenceng.lazyclaude.session.domain.Watched;

/** The user looks at no live session. */
public record NoWatchedSessionPayload() implements WatchedPayload {

    @Override
    public Watched toDomain() {
        return new NoWatchedSession();
    }
}

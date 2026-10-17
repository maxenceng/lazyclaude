package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.Watched;

/** What {@link WatchedSessionCommand} is told: the session the user looks at, or that there is none. */
public sealed interface WatchedSessionPayload permits WatchingSessionPayload, NoWatchedSessionPayload {

    Watched toDomain();
}

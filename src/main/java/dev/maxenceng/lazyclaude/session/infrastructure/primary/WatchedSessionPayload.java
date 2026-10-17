package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.Watched;
import dev.maxenceng.lazyclaude.session.domain.WatchedSession;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The user looks at this live session: its dock tab is selected while the dock and the terminal have focus. */
public record WatchedSessionPayload(SessionId session) implements WatchedPayload {

    public WatchedSessionPayload {
        Assert.notNull("session id", session);
    }

    @Override
    public Watched toDomain() {
        return new WatchedSession(session);
    }
}

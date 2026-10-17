package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The user is looking at this session, so it is never notified. */
public record WatchedSession(SessionId session) implements Watched {

    public WatchedSession {
        Assert.notNull("session", session);
    }

    @Override
    public boolean watches(SessionId other) {
        return session.equals(other);
    }
}

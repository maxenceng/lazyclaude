package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.time.LocalDateTime;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The local time an age is measured from, as a session view carries it: a live session's start, a past one's last activity. */
public record SessionAgePayload(LocalDateTime since) {

    public SessionAgePayload {
        Assert.notNull("since", since);
    }

    /** Measures how long {@code session} has run. */
    public static SessionAgePayload startOf(LiveSessionView session) {
        return new SessionAgePayload(session.startedAt());
    }

    /** Measures how long ago {@code session} was last active. */
    public static SessionAgePayload lastActivityOf(PastSessionView session) {
        return new SessionAgePayload(session.lastActivity());
    }
}

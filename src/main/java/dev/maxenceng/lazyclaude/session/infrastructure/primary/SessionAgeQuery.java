package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;

/**
 * How long ago a session started or was last active, asked when its row is drawn rather than carried on its view, so the desktop
 * window, which shows no age, is sent none.
 */
public final class SessionAgeQuery {

    private final SessionApplicationService sessions;

    public SessionAgeQuery(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** The time elapsed since the payload's local time, measured now. */
    public SessionAgeView ageSince(SessionAgePayload payload) {
        return new SessionAgeView(sessions.ageSince(payload.since()));
    }
}

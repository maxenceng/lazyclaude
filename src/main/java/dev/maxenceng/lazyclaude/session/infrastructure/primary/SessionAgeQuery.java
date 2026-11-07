package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.published.SessionAge;

/**
 * How long ago a session started or was last active, asked when its row is drawn rather than carried on its view, so the desktop
 * window, which shows no age, is sent none.
 */
public final class SessionAgeQuery {

    private final SessionApplicationService sessions;

    public SessionAgeQuery(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** How long the live session has run. */
    public SessionAge startedAgo(LiveSessionView session) {
        return sessions.ageSince(session.startedAt());
    }

    /** How long ago the past session was last active. */
    public SessionAge lastActiveAgo(PastSessionView session) {
        return sessions.ageSince(session.lastActivity());
    }
}

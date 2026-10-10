package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.LiveSessions;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.UnavailableSessions;

/** A project's sessions as the shell reads them: the live ones, or why none could be read. */
public sealed interface SessionsView permits LiveSessionsView, UnavailableSessionsView {

    static SessionsView from(Sessions sessions) {
        return switch (sessions) {
            case LiveSessions live -> LiveSessionsView.from(live);
            case UnavailableSessions unavailable -> UnavailableSessionsView.from(unavailable);
        };
    }
}

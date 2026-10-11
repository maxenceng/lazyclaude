package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.UnavailableSessions;

/** A project's sessions as the shell reads them: the live and past ones, or why none could be read. */
public sealed interface SessionsView permits AvailableSessionsView, UnavailableSessionsView {

    static SessionsView from(Sessions sessions) {
        return switch (sessions) {
            case AvailableSessions available -> AvailableSessionsView.from(available);
            case UnavailableSessions unavailable -> UnavailableSessionsView.from(unavailable);
        };
    }
}

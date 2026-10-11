package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.UnavailableSessions;

/** A project's sessions as the shell reads them: the live and past ones, or why none could be read. */
public sealed interface SessionsView permits AvailableSessionsView, UnavailableSessionsView {

    /** The hosted sessions, in tab order: lazyclaude's own, so they are there whether or not the registry could be read. */
    List<HostedSessionView> hosted();

    static SessionsView from(Sessions sessions) {
        return switch (sessions) {
            case AvailableSessions available -> AvailableSessionsView.from(available);
            case UnavailableSessions unavailable -> UnavailableSessionsView.from(unavailable);
        };
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.UnavailableSessions;
import dev.maxenceng.lazyclaude.session.published.Reason;

/** Sessions that could not be read, and why. */
public record UnavailableSessionsView(Reason reason) implements SessionsView {

    static UnavailableSessionsView from(UnavailableSessions sessions) {
        return new UnavailableSessionsView(sessions.reason());
    }
}

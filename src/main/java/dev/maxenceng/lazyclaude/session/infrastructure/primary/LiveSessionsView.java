package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.LiveSessions;

/** The project's live sessions, newest first. */
public record LiveSessionsView(List<SessionView> sessions) implements SessionsView {

    static LiveSessionsView from(LiveSessions live) {
        return new LiveSessionsView(live.sessions().stream()
                .map(session -> SessionView.from(live, session))
                .toList());
    }
}

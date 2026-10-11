package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;

/** What the shell asks of {@code session}: a project's live and past sessions as a view, and a view again whenever they may have changed. It only reads. */
public final class SessionQuery {

    private final SessionApplicationService sessions;

    public SessionQuery(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** The project's live and past sessions, or why none could be read; never a failure. */
    public SessionsView sessionsFor(SessionPayload payload) {
        return SessionsView.from(sessions.sessionsFor(payload.toDomain()));
    }

    /** Hands {@code listener} a view on each change and each sweep, on a thread of the watcher's own, not the caller's. */
    public void subscribe(SessionPayload payload, Consumer<SessionsView> listener) {
        sessions.subscribe(payload.toDomain(), read -> listener.accept(SessionsView.from(read)));
    }
}

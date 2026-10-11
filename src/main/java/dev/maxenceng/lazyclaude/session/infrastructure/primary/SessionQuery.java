package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;

/** What the shell asks of {@code session}: a project's live and past sessions as a view, and a view again whenever they may have changed. It only reads: the hosted sessions' screens included. */
public final class SessionQuery {

    private final SessionApplicationService sessions;

    public SessionQuery(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** The project's live and past sessions, or why none could be read; never a failure. */
    public SessionsView sessionsFor(SessionPayload payload) {
        return SessionsView.from(sessions.sessionsFor(payload.toDomain(), payload.tickets()));
    }

    /** Hands {@code listener} a view on each change and each sweep, on a thread of the watcher's own, not the caller's. */
    public void subscribe(SessionPayload payload, Consumer<SessionsView> listener) {
        sessions.subscribe(payload.toDomain(), payload.tickets(), read -> listener.accept(SessionsView.from(read)));
    }

    /** What a hosted session shows right now, blank for one that is gone. */
    public TerminalScreen screenOf(HostedSessionId id) {
        return sessions.screenOf(id);
    }

    /** Tells {@code listener} whenever a hosted session drew something, on a thread of the host's own. */
    public void onScreenChange(Runnable listener) {
        sessions.onScreenChange(listener);
    }
}

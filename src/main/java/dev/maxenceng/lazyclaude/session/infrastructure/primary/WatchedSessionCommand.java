package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;

/** What the shell tells {@code session} about where the user is looking, so that the session in view is never notified. */
public final class WatchedSessionCommand {

    private final SessionApplicationService sessions;

    public WatchedSessionCommand(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** From now on the payload's session is the one in view; any notification it has is closed. May run a process, so not on the render thread. */
    public void watching(WatchedPayload payload) {
        sessions.watching(payload.toDomain());
    }
}

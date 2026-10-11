package dev.maxenceng.lazyclaude.session.application;

import java.time.Clock;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionClock;
import dev.maxenceng.lazyclaude.session.domain.SessionManager;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/** Reads a project's live sessions, and keeps a listener handed them again as they change. */
public final class SessionApplicationService {

    private final SessionManager manager;

    public SessionApplicationService(
            LiveSessionPort sessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            WarningPort warnings,
            Clock clock) {
        this.manager = new SessionManager(sessions, locator, watch, warnings, new SessionClock(clock));
    }

    public Sessions sessionsFor(Project project) {
        return manager.sessionsFor(project);
    }

    public void subscribe(Project project, Consumer<Sessions> listener) {
        manager.watch(() -> listener.accept(manager.sessionsFor(project)));
    }
}

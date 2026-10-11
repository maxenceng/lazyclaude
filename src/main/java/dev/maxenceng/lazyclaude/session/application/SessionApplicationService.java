package dev.maxenceng.lazyclaude.session.application;

import java.time.Clock;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionClock;
import dev.maxenceng.lazyclaude.session.domain.SessionManager;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/** Reads a project's live and past sessions, and keeps a listener handed them again as they change. */
public final class SessionApplicationService {

    private final SessionManager manager;

    public SessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort pastSessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            Clock clock) {
        this.manager = new SessionManager(
                sessions, pastSessions, locator, watch, transcriptWatch, warnings, new SessionClock(clock));
    }

    public Sessions sessionsFor(Project project) {
        return manager.sessionsFor(project);
    }

    public void subscribe(Project project, Consumer<Sessions> listener) {
        manager.watch(project, listener);
    }
}

package dev.maxenceng.lazyclaude.session.application;

import java.time.Clock;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.domain.KnownTickets;
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

/** Reads a project's live and past sessions, linked to the tickets it is handed, and keeps a listener handed them again as they change. */
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

    public Sessions sessionsFor(Project project, KnownTickets tickets) {
        return manager.sessionsFor(project, tickets);
    }

    public void subscribe(Project project, KnownTickets tickets, Consumer<Sessions> listener) {
        manager.watch(project, tickets, listener);
    }
}

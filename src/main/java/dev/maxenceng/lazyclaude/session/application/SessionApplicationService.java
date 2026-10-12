package dev.maxenceng.lazyclaude.session.application;

import java.time.Clock;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.HostedSessionManager;
import dev.maxenceng.lazyclaude.session.domain.KnownTickets;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionManager;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Reads a project's live, past and hosted sessions, linked to the tickets it is handed, and keeps a listener handed them
 * again as they change; and starts, feeds and ends the hosted ones.
 */
public final class SessionApplicationService {

    private final SessionManager manager;
    private final HostedSessionManager hosted;

    public SessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort pastSessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts,
            Clock clock) {
        this.hosted = new HostedSessionManager(hosts, warnings);
        this.manager = new SessionManager(sessions, pastSessions, locator, watch, transcriptWatch, warnings, clock);
    }

    public Sessions sessionsFor(Project project, KnownTickets tickets) {
        return manager.sessionsFor(project, tickets).hosting(hosted.open());
    }

    public void subscribe(Project project, KnownTickets tickets, Consumer<Sessions> listener) {
        Consumer<Sessions> hosting = sessions -> listener.accept(sessions.hosting(hosted.open()));
        manager.watch(project, tickets, hosting);
        hosted.onChange(() -> manager.refresh(project, tickets, hosting));
    }

    public HostStart startUnlinked(Project project, PaneSize size) {
        return hosted.startUnlinked(project, size);
    }

    public HostStart resume(SessionId session, WorkingDirectory recorded, PaneSize size) {
        return hosted.resume(session, recorded, size);
    }

    public void send(HostedSessionId id, TerminalInput input) {
        hosted.send(id, input);
    }

    public void resize(HostedSessionId id, PaneSize size) {
        hosted.resize(id, size);
    }

    public TerminalScreen screenOf(HostedSessionId id) {
        return hosted.screenOf(id);
    }

    public void onScreenChange(Runnable listener) {
        hosted.onScreenChange(listener);
    }

    public void endAll() {
        hosted.endAll();
    }
}

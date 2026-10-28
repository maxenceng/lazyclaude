package dev.maxenceng.lazyclaude.session.application;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.review.published.MergedHead;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationManager;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.HeldWorktree;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.HostedSessionManager;
import dev.maxenceng.lazyclaude.session.domain.KnownTickets;
import dev.maxenceng.lazyclaude.session.domain.LastPushedSessions;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionManager;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.TicketDoneEndedListener;
import dev.maxenceng.lazyclaude.session.domain.TicketStart;
import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.VerbChoice;
import dev.maxenceng.lazyclaude.session.domain.Watched;
import dev.maxenceng.lazyclaude.session.domain.WorktreeManager;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotClearException;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
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
 * again as they change, notifying the desktop of the project's sessions that enter waiting; and starts, feeds and ends the hosted ones.
 */
public final class SessionApplicationService {

    private final SessionManager manager;
    private final HostedSessionManager hosted;
    private final WorktreeManager worktreeing;
    private final DesktopNotificationManager notifying;
    private final boolean desktopNotifications;
    private final LastPushedSessions lastPushed = new LastPushedSessions();
    private final AtomicReference<Consumer<Sessions>> delivering = new AtomicReference<>(sessions -> {});

    public SessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort pastSessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts,
            WorktreePort worktrees,
            DesktopNotificationPort notifications,
            boolean desktopNotifications,
            Clock clock) {
        this.hosted = new HostedSessionManager(hosts, warnings, worktrees);
        this.worktreeing = new WorktreeManager(worktrees);
        this.notifying = new DesktopNotificationManager(notifications, warnings);
        this.desktopNotifications = desktopNotifications;
        this.manager = new SessionManager(sessions, pastSessions, locator, watch, transcriptWatch, warnings, clock);
    }

    public Sessions sessionsFor(Project project, KnownTickets tickets) {
        return manager.sessionsFor(project, tickets).hosting(hosted.open());
    }

    public void subscribe(Project project, KnownTickets tickets, Consumer<Sessions> listener) {
        Consumer<Sessions> hosting = sessions -> {
            lastPushed.remember(sessions);
            Sessions hostedOnly = sessions.hosting(hosted.open());
            notifying.seen(project, hostedOnly, desktopNotifications);
            listener.accept(hostedOnly);
        };
        manager.watch(project, tickets, hosting);
        hosted.onChange(() -> manager.refresh(project, hosting));
        delivering.set(hosting);
    }

    /** From now on {@code watched} is the session the user looks at: it is not notified, and any notification it has is closed. */
    public void watching(Watched watched) {
        notifying.watching(watched);
    }

    /** From now on sessions are linked against {@code tickets}, the ids of the board as it was last read; the subscriber is handed them linked that way at once. */
    public void knowTickets(Project project, KnownTickets tickets) {
        manager.know(project, tickets, delivering.get());
    }

    /** Reads the live sessions and the transcripts again now, linked against {@code tickets}, rather than at the next change or sweep. */
    public void refresh(Project project, KnownTickets tickets) {
        manager.know(project, tickets, delivering.get());
        manager.rescanTranscripts();
    }

    public HostStart startUnlinked(Project project, PaneSize size) {
        return hosted.startUnlinked(project, size);
    }

    /** A session running {@code verb} on a ticket, in the ticket's worktree, added first when it is missing. */
    public HostStart startTicket(TicketStart ticket, PaneSize size) {
        return hosted.startTicket(ticket, size);
    }

    public HostStart resumeInTicketWorktree(
            SessionId session, WorkingDirectory recorded, TicketWorktree worktree, PaneSize size) {
        return hosted.resumeInTicketWorktree(session, recorded, worktree, size);
    }

    /**
     * The verbs a ticket allows and the one to start on: {@code design} while no session has run it, {@code done} once the branch
     * is merged. The sessions are the watcher's last push, with the hosted ones as they are now, so the render thread asking
     * on every move never waits on the registry; only before the first push are they read here. A registry that cannot be
     * read leaves only the hosted ones to go by.
     */
    // deferred: a session that ran design counts one watcher tick late unless lazyclaude hosts it — close it when a preselection is seen stale.
    public VerbChoice verbChoiceFor(
            Project project, KnownTickets tickets, LinkedTicketId ticket, List<Verb> allowed, boolean merged) {
        return VerbChoice.of(allowed, sessionsAsPushed(project, tickets).ranVerb(ticket, Verb.DESIGN), merged);
    }

    private Sessions sessionsAsPushed(Project project, KnownTickets tickets) {
        return lastPushed.last().map(pushed -> pushed.hosting(hosted.open())).orElseGet(() -> sessionsFor(project, tickets));
    }

    /**
     * The ticket worktree and what stands in it, none when the ticket has no worktree. The sessions are the watcher's last push with
     * the hosted ones as they are now, as {@link #verbChoiceFor} reads them. It runs {@code git}, so not on the render thread.
     */
    public Optional<HeldWorktree> worktreeOf(Project project, KnownTickets tickets, TicketWorktree worktree, MergedHead merged) {
        return worktreeing.held(worktree, merged, sessionsAsPushed(project, tickets));
    }

    /**
     * Removes the ticket worktree, keeping its branch, after judging it again; nothing when it has none.
     *
     * @throws WorktreeNotClearException when something stands in it
     */
    public void removeWorktree(Project project, KnownTickets tickets, TicketWorktree worktree, MergedHead merged) {
        worktreeing.remove(worktree, merged, sessionsAsPushed(project, tickets));
    }

    /** Tells {@code listener} which ticket's {@code /ticket done} session ended, however it ended. */
    public void onTicketDoneEnded(TicketDoneEndedListener listener) {
        hosted.onTicketDoneEnded(listener);
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

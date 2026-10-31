package dev.maxenceng.lazyclaude.session.application;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.review.published.MergedHead;
import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationManager;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.HeldWorktree;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.HostedSessionManager;
import dev.maxenceng.lazyclaude.session.domain.KnownTickets;
import dev.maxenceng.lazyclaude.session.domain.LastPushedSessions;
import dev.maxenceng.lazyclaude.session.domain.LaunchLogPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.domain.PtyListener;
import dev.maxenceng.lazyclaude.session.domain.SessionExitListener;
import dev.maxenceng.lazyclaude.session.domain.SessionManager;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TicketDoneEndedListener;
import dev.maxenceng.lazyclaude.session.domain.TicketStart;
import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.session.domain.TimerPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.VerbChoice;
import dev.maxenceng.lazyclaude.session.domain.Watched;
import dev.maxenceng.lazyclaude.session.domain.WorktreeManager;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotClearException;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Reads a project's hosted and past sessions, linked to the tickets it is handed, and keeps a listener handed them
 * again as they change, notifying the desktop of the hosted sessions that enter waiting; and starts, feeds and ends the hosted ones.
 */
public final class SessionApplicationService {

    private final SessionManager manager;
    private final HostedSessionManager hosted;
    private final WorktreeManager worktreeing;
    private final DesktopNotificationManager notifying;
    private final boolean desktopNotifications;
    private final LastPushedSessions lastPushed = new LastPushedSessions();
    private final AtomicReference<Consumer<AvailableSessions>> delivering = new AtomicReference<>(sessions -> {});

    public SessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort pastSessions,
            LaunchLogPort launchLog,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts,
            WorktreePort worktrees,
            DesktopNotificationPort notifications,
            boolean desktopNotifications,
            TimerPort timer,
            Clock clock) {
        this.hosted = new HostedSessionManager(hosts, sessions, warnings, worktrees, launchLog, timer, clock);
        this.worktreeing = new WorktreeManager(worktrees);
        this.notifying = new DesktopNotificationManager(notifications, warnings);
        this.desktopNotifications = desktopNotifications;
        this.manager = new SessionManager(sessions, pastSessions, launchLog, watch, transcriptWatch, warnings, clock);
    }

    public AvailableSessions sessionsFor(Project project, KnownTickets tickets) {
        return manager.sessionsFor(project, tickets, hosted.open());
    }

    public void subscribe(Project project, KnownTickets tickets, Consumer<AvailableSessions> listener) {
        Consumer<AvailableSessions> hosting = sessions -> {
            lastPushed.remember(sessions);
            notifying.seen(project, sessions, desktopNotifications);
            listener.accept(sessions);
        };
        manager.watch(project, tickets, hosted::open, hosting);
        hosted.onChange(() -> manager.refresh(project, hosted.open(), hosting));
        delivering.set(hosting);
    }

    /** From now on {@code watched} is the session the user looks at: it is not notified, and any notification it has is closed. */
    public void watching(Watched watched) {
        notifying.watching(watched);
    }

    /** From now on sessions are linked against {@code tickets}, the ids of the board as it was last read; the subscriber is handed them linked that way at once. */
    public void knowTickets(Project project, KnownTickets tickets) {
        manager.know(project, tickets, hosted.open(), delivering.get());
    }

    /** Reads the live sessions and the transcripts again now, linked against {@code tickets}, rather than at the next change or sweep. */
    public void refresh(Project project, KnownTickets tickets) {
        manager.know(project, tickets, hosted.open(), delivering.get());
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
     * is merged. The sessions are the watcher's last push, so the render thread asking on every move never waits on the
     * registry; only before the first push are they read here.
     */
    public VerbChoice verbChoiceFor(
            Project project, KnownTickets tickets, LinkedTicketId ticket, List<Verb> allowed, boolean merged) {
        return VerbChoice.of(allowed, sessionsAsPushed(project, tickets).ranVerb(ticket, Verb.DESIGN), merged);
    }

    private AvailableSessions sessionsAsPushed(Project project, KnownTickets tickets) {
        return lastPushed.last().orElseGet(() -> sessionsFor(project, tickets));
    }

    /**
     * The ticket worktree and what stands in it, none when the ticket has no worktree. The sessions are the watcher's last push, as
     * {@link #verbChoiceFor} reads them. It runs {@code git}, so not on the render thread.
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

    public HostStart resume(Project project, SessionId session, WorkingDirectory recorded, PaneSize size) {
        return hosted.resume(project, session, recorded, size);
    }

    public void write(SessionId id, PtyBytes bytes) {
        hosted.write(id, bytes);
    }

    public void resize(SessionId id, PaneSize size) {
        hosted.resize(id, size);
    }

    public void onPty(PtyListener listener) {
        hosted.onPty(listener);
    }

    public void repaint(SessionId id) {
        hosted.repaint(id);
    }

    public void onExit(SessionExitListener listener) {
        hosted.onExit(listener);
    }

    public void endAll() {
        hosted.endAll();
    }
}

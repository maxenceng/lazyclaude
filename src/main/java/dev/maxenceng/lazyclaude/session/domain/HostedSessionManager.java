package dev.maxenceng.lazyclaude.session.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.PaneHeight;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Holds the hosted sessions that are open, in start order, and decides where each runs: a new unlinked session in the
 * project root, a resumed one in the directory it was launched in, and none with a warning when that directory is
 * gone, since {@code claude --resume} anywhere else would not find the transcript. Each session that starts is appended to the
 * project's launch log, and one whose {@code claude} exits by itself has its end recorded there; a quit that ends it records
 * none. A launch log that cannot be changed is said once and never stops a session. A session whose {@code claude} exits is
 * dropped, with a warning when the exit was not clean; one that could not be started is never held, and says why.
 */
public final class HostedSessionManager {

    private static final Duration REPAINT_GAP = Duration.ofMillis(50);
    private static final String START_WARNING = "hosted session: %s.";
    private static final String EXIT_WARNING = "hosted session: claude exited with code %d.";
    private static final String GONE_WARNING = "resume: %s is gone; nothing resumed.";
    private static final String WORKTREE_WARNING = "ticket worktree: %s; nothing started.";
    private static final String NOT_A_WORKTREE = "%s exists but is not a worktree of %s";
    private static final String NO_PLACE = "%s has no folder beside it to keep worktrees in";

    private final HostPort hosts;
    private final LiveSessionPort records;
    private final WarningPort warnings;
    private final WorktreePort worktrees;
    private final LaunchLogPort log;
    private final TimerPort timer;
    private final SessionClock clock;
    private final List<HostedSession> open = new ArrayList<>();
    private final List<SessionId> spawning = new ArrayList<>();
    private final List<SessionId> endingWithLazyclaude = new CopyOnWriteArrayList<>();
    private final Map<SessionId, ExitCode> earlyExits = new HashMap<>();
    private final Map<SessionId, PaneSize> sizes = new ConcurrentHashMap<>();
    private final List<OpenSessionsListener> changeListeners = new CopyOnWriteArrayList<>();
    private final List<TicketDoneEndedListener> doneEndedListeners = new CopyOnWriteArrayList<>();
    private final List<PtyListener> ptyListeners = new CopyOnWriteArrayList<>();
    private final List<SessionExitListener> exitListeners = new CopyOnWriteArrayList<>();

    public HostedSessionManager(
            HostPort hosts,
            LiveSessionPort records,
            WarningPort warnings,
            WorktreePort worktrees,
            LaunchLogPort log,
            TimerPort timer,
            Clock clock) {
        this.hosts = hosts;
        this.records = records;
        this.warnings = warnings;
        this.worktrees = worktrees;
        this.log = log;
        this.timer = timer;
        this.clock = new SessionClock(clock);
    }

    /** A plain {@code claude} in the project root. */
    public HostStart startUnlinked(Project project, PaneSize size) {
        return start(project, new NewClaude(SessionId.random()), new WorkingDirectory(project.root()), size);
    }

    /**
     * {@code /ticket <verb> <ID>} in the ticket's worktree: where its branch is already checked out, else a worktree added at the
     * path the project keeps it at, on the branch when it exists and on a new one from {@code main} when not. None, with a warning,
     * when that path is taken by something else or git refuses.
     */
    public HostStart startTicket(TicketStart ticket, PaneSize size) {
        try {
            return start(
                    ticket.worktree().project(),
                    TicketClaude.builder().session(SessionId.random()).ticket(ticket.ticket()).verb(ticket.verb()).build(),
                    ticketDirectory(ticket.worktree()),
                    size);
        } catch (WorktreeUnavailableException unavailable) {
            return notStartedFor(unavailable.getMessage());
        }
    }

    /**
     * Like {@link #resume}, but a {@code recorded} directory that is {@code worktree}'s own path and gone is added back first, so
     * resuming a ticket's session after its worktree was removed finds its transcript again.
     */
    public HostStart resumeInTicketWorktree(
            SessionId session, WorkingDirectory recorded, TicketWorktree worktree, PaneSize size) {
        boolean gone = !hosts.exists(recorded);
        if (gone && worktree.path().filter(recorded::equals).isPresent()) {
            try {
                addWorktree(worktree, recorded);
            } catch (WorktreeUnavailableException unavailable) {
                return notStartedFor(unavailable.getMessage());
            }
        }
        return resume(worktree.project(), session, recorded, size);
    }

    /**
     * {@code claude --resume} of {@code session}, in {@code recorded} when it still exists, else nothing; the open
     * session already running as it when there is one, since two {@code claude} on one transcript would both write to it. An open
     * session runs as the id it was launched with until its registry record names another, as it does after {@code /clear}.
     */
    public HostStart resume(Project project, SessionId session, WorkingDirectory recorded, PaneSize size) {
        return open().sessions().stream()
                .filter(hosted -> hosted.id().equals(session) || runningNow(hosted).filter(session::equals).isPresent())
                .findFirst()
                .<HostStart>map(running -> new StartedHost(running.id()))
                .orElseGet(() -> resumeAt(project, session, recorded, size));
    }

    /** None while its record is not written, is another process's, or cannot be read: then it runs as the id it was launched with. */
    private Optional<SessionId> runningNow(HostedSession hosted) {
        try {
            return records.recordOf(hosted.pid()).filter(hosted::reportedBy).map(ReportedSession::session);
        } catch (SessionRecordUnreadableException unreadable) {
            return Optional.empty();
        }
    }

    private HostStart resumeAt(Project project, SessionId session, WorkingDirectory recorded, PaneSize size) {
        if (!hosts.exists(recorded)) {
            warnings.raise(new Warning(GONE_WARNING.formatted(recorded.path())));
            return new NotStartedHost();
        }
        return start(project, new ResumeClaude(session), recorded, size);
    }

    public HostedSessions open() {
        synchronized (open) {
            return new HostedSessions(open.stream().toList());
        }
    }

    /** Writes {@code bytes}, already encoded by the frontend, to the session's pty. */
    public void write(SessionId id, PtyBytes bytes) {
        hosts.write(id, bytes);
    }

    /** Resizing takes the lock the repaint's restore does, so a restore never applies a size older than one applied meanwhile. */
    public void resize(SessionId id, PaneSize size) {
        synchronized (sizes) {
            sizes.replace(id, size);
            hosts.resize(id, size);
        }
    }

    /**
     * Makes {@code claude} redraw its whole screen for a frontend whose emulator starts empty (ADR 0049): the session's pty is shrunk
     * by one row and, {@link #REPAINT_GAP} later, given back the size it has by then, as two resizes with no gap repaint nothing.
     * Nothing for a session that is not open.
     */
    public void repaint(SessionId id) {
        synchronized (sizes) {
            Optional.ofNullable(sizes.get(id)).ifPresent(size -> {
                hosts.resize(id, oneRowShorter(size));
                timer.after(REPAINT_GAP, () -> restored(id));
            });
        }
    }

    private void restored(SessionId id) {
        synchronized (sizes) {
            Optional.ofNullable(sizes.get(id)).ifPresent(size -> hosts.resize(id, size));
        }
    }

    // deferred: a one-row pane is not shrunk, so a repaint of it changes nothing — close it when a pane that small is hosted.
    private static PaneSize oneRowShorter(PaneSize size) {
        return PaneSize.builder().width(size.width()).height(new PaneHeight(size.height().cells() - 1)).build();
    }

    /** Ends every open session's {@code claude}: they live and die with lazyclaude. */
    public void endAll() {
        List<SessionId> ending = open().sessions().stream().map(HostedSession::id).toList();
        endingWithLazyclaude.addAll(ending);
        ending.forEach(hosts::end);
    }

    /** Tells {@code listener} whenever a session opens or closes. */
    public void onChange(OpenSessionsListener listener) {
        changeListeners.add(listener);
    }

    /** Tells {@code listener} which ticket's {@code /ticket done} session ended, however it ended; a session running any other verb tells nothing. */
    public void onTicketDoneEnded(TicketDoneEndedListener listener) {
        doneEndedListeners.add(listener);
    }

    /** Tells {@code listener} the code of each session whose {@code claude} exits by itself; a session ended with lazyclaude tells nothing. */
    public void onExit(SessionExitListener listener) {
        exitListeners.add(listener);
    }

    /** Tells {@code listener} what happens to each session's pty from now on. */
    public void onPty(PtyListener listener) {
        ptyListeners.add(listener);
    }

    private WorkingDirectory ticketDirectory(TicketWorktree worktree) {
        return worktrees.checkedOutAt(worktree.project(), worktree.branch()).orElseGet(() -> addedWorktree(worktree));
    }

    private WorkingDirectory addedWorktree(TicketWorktree worktree) {
        WorkingDirectory planned = worktree.path()
                .orElseThrow(() -> new WorktreeUnavailableException(NO_PLACE.formatted(worktree.project().root())));
        if (hosts.exists(planned)) {
            throw new WorktreeUnavailableException(NOT_A_WORKTREE.formatted(planned.path(), worktree.branch().value()));
        }
        addWorktree(worktree, planned);
        return planned;
    }

    private void addWorktree(TicketWorktree worktree, WorkingDirectory at) {
        if (worktrees.branchExists(worktree.project(), worktree.branch())) {
            worktrees.addOnBranch(worktree.project(), worktree.branch(), at);
        } else {
            worktrees.addOnNewBranch(worktree.project(), worktree.branch(), at);
        }
    }

    private HostStart notStartedFor(String reason) {
        warnings.raise(new Warning(WORKTREE_WARNING.formatted(reason)));
        return new NotStartedHost();
    }

    /**
     * Claims {@code command}'s id under the lock before spawning, so two resumes of one session racing on the shell's
     * background threads start one {@code claude}: the loser answers with the open session, or with nothing while the
     * winner is still spawning, since its tab opens when that spawn returns. The spawn instant is taken before spawning and to the
     * millisecond, as the registry records it, so the session's own record is never older than its spawn.
     */
    private HostStart start(Project project, LaunchCommand command, WorkingDirectory directory, PaneSize size) {
        SessionId id = command.session();
        HostLaunch launch = HostLaunch.builder()
                .command(command)
                .directory(directory)
                .size(size)
                .build();
        synchronized (open) {
            if (spawning.contains(id)) {
                return new NotStartedHost();
            }
            if (open.stream().anyMatch(session -> session.id().equals(id))) {
                return new StartedHost(id);
            }
            spawning.add(id);
            sizes.put(id, size);
        }
        StartedAt spawnedAt = new StartedAt(clock.clock().instant().truncatedTo(ChronoUnit.MILLIS));
        try {
            ptyListeners.forEach(listener -> listener.launching(id, size));
            ProcessId pid = hosts.spawn(
                    launch, bytes -> ptyListeners.forEach(listener -> listener.output(id, bytes)), code -> exited(project, id, code));
            return registered(project, HostedSession.builder()
                    .pid(pid)
                    .directory(directory)
                    .command(command)
                    .spawnedAt(spawnedAt)
                    .build());
        } catch (HostUnavailableException unavailable) {
            ended(id);
            synchronized (open) {
                spawning.remove(id);
            }
            warnings.raise(new Warning(START_WARNING.formatted(unavailable.getMessage())));
            return new NotStartedHost();
        }
    }

    /**
     * A claude that died before its spawn returned is never held: nothing would ever close its tab. One that lives is opened and
     * logged under one lock, so its exit, which takes that lock to find it, records its end after its launch and never before; and
     * it is open before it is logged, so a read of the log that finds it reads the open sessions with it.
     */
    private HostStart registered(Project project, HostedSession session) {
        Optional<ExitCode> earlyExit;
        synchronized (open) {
            spawning.remove(session.id());
            earlyExit = Optional.ofNullable(earlyExits.remove(session.id()));
            if (earlyExit.isEmpty()) {
                open.add(session);
                // deferred: only the launch id is logged, so the session a /clear starts is never a past session and its /ticket runs link nothing — close it when that conversation is missed from the past block.
                logged(project, entries -> entries.launched(entryOf(session)));
            }
        }
        return earlyExit.<HostStart>map(this::notStartedAfter).orElseGet(() -> startedAs(session));
    }

    private HostStart notStartedAfter(ExitCode exit) {
        warnOfFailure(exit);
        return new NotStartedHost();
    }

    private HostStart startedAs(HostedSession session) {
        changed();
        return new StartedHost(session.id());
    }

    private void exited(Project project, SessionId id, ExitCode code) {
        ended(id);
        Optional<HostedSession> dropped;
        synchronized (open) {
            dropped = open.stream().filter(session -> session.id().equals(id)).findFirst();
            dropped.ifPresent(open::remove);
            if (dropped.isEmpty() && spawning.contains(id)) {
                earlyExits.put(id, code);
            }
        }
        if (dropped.isEmpty()) {
            return;
        }
        boolean byItself = !endingWithLazyclaude.remove(id);
        if (byItself) {
            EndedAt ended = new EndedAt(clock.clock().instant());
            logged(project, entries -> entries.ended(id, ended));
        }
        warnOfFailure(code);
        changed();
        if (byItself) {
            exitListeners.forEach(listener -> listener.exited(id, code));
        }
        dropped.flatMap(HostedSession::doneTicket).ifPresent(this::doneEnded);
    }

    private void ended(SessionId id) {
        sizes.remove(id);
        ptyListeners.forEach(listener -> listener.ended(id));
    }

    private static LaunchEntry entryOf(HostedSession session) {
        return LaunchEntry.builder()
                .command(session.command())
                .directory(session.directory())
                .startedAt(session.spawnedAt())
                .build();
    }

    private void logged(Project project, UnaryOperator<LaunchLog> change) {
        log.change(project, this::warnOfLog, change);
    }

    private void warnOfLog(Reason cause) {
        warnings.raise(LaunchLogWarning.of(cause));
    }

    private void doneEnded(LinkedTicketId ticket) {
        doneEndedListeners.forEach(listener -> listener.ended(ticket));
    }

    private void changed() {
        changeListeners.forEach(OpenSessionsListener::changed);
    }

    private void warnOfFailure(ExitCode code) {
        if (code.isFailure()) {
            warnings.raise(new Warning(EXIT_WARNING.formatted(code.value())));
        }
    }
}

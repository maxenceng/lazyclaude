package dev.maxenceng.lazyclaude.session.domain;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Holds the hosted sessions that are open, in start order, and decides where each runs: a new unlinked session in the
 * project root, a resumed one in the directory its transcript recorded, and none with a warning when that directory is
 * gone, since {@code claude --resume} anywhere else would not find the transcript. A session whose {@code claude} exits is dropped, with a warning when the exit was not clean; one that could not
 * be started is never held, and says why.
 */
public final class HostedSessionManager {

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
    private final SessionClock clock;
    private final List<HostedSession> open = new ArrayList<>();
    private final List<SessionId> spawning = new ArrayList<>();
    private final Map<SessionId, ExitCode> earlyExits = new HashMap<>();
    private final List<OpenSessionsListener> changeListeners = new CopyOnWriteArrayList<>();
    private final List<TicketDoneEndedListener> doneEndedListeners = new CopyOnWriteArrayList<>();

    public HostedSessionManager(HostPort hosts, LiveSessionPort records, WarningPort warnings, WorktreePort worktrees, Clock clock) {
        this.hosts = hosts;
        this.records = records;
        this.warnings = warnings;
        this.worktrees = worktrees;
        this.clock = new SessionClock(clock);
    }

    /** A plain {@code claude} in the project root. */
    public HostStart startUnlinked(Project project, PaneSize size) {
        return start(new NewClaude(SessionId.random()), new WorkingDirectory(project.root()), size);
    }

    /**
     * {@code /ticket <verb> <ID>} in the ticket's worktree: where its branch is already checked out, else a worktree added at the
     * path the project keeps it at, on the branch when it exists and on a new one from {@code main} when not. None, with a warning,
     * when that path is taken by something else or git refuses.
     */
    public HostStart startTicket(TicketStart ticket, PaneSize size) {
        try {
            return start(
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
        return resume(session, recorded, size);
    }

    /**
     * {@code claude --resume} of {@code session}, in {@code recorded} when it still exists, else nothing; the open
     * session already running as it when there is one, since two {@code claude} on one transcript would both write to it. An open
     * session runs as the id it was launched with until its registry record names another, as it does after {@code /clear}.
     */
    public HostStart resume(SessionId session, WorkingDirectory recorded, PaneSize size) {
        return open().sessions().stream()
                .filter(hosted -> hosted.id().equals(session) || runningNow(hosted).filter(session::equals).isPresent())
                .findFirst()
                .<HostStart>map(running -> new StartedHost(running.id()))
                .orElseGet(() -> resumeAt(session, recorded, size));
    }

    /** None while its record is not written, is another process's, or cannot be read: then it runs as the id it was launched with. */
    private Optional<SessionId> runningNow(HostedSession hosted) {
        try {
            return records.recordOf(hosted.pid()).filter(hosted::reportedBy).map(ReportedSession::session);
        } catch (SessionRecordUnreadableException unreadable) {
            return Optional.empty();
        }
    }

    private HostStart resumeAt(SessionId session, WorkingDirectory recorded, PaneSize size) {
        if (!hosts.exists(recorded)) {
            warnings.raise(new Warning(GONE_WARNING.formatted(recorded.path())));
            return new NotStartedHost();
        }
        return start(new ResumeClaude(session), recorded, size);
    }

    public HostedSessions open() {
        synchronized (open) {
            return new HostedSessions(open.stream().toList());
        }
    }

    public void send(SessionId id, TerminalInput input) {
        hosts.send(id, input);
    }

    public void resize(SessionId id, PaneSize size) {
        hosts.resize(id, size);
    }

    public TerminalScreen screenOf(SessionId id) {
        return hosts.screenOf(id);
    }

    /** Ends every open session's {@code claude}: they live and die with lazyclaude. */
    public void endAll() {
        open().sessions().forEach(session -> hosts.end(session.id()));
    }

    /** Tells {@code listener} whenever a session opens or closes. */
    public void onChange(OpenSessionsListener listener) {
        changeListeners.add(listener);
    }

    /** Tells {@code listener} which ticket's {@code /ticket done} session ended, however it ended; a session running any other verb tells nothing. */
    public void onTicketDoneEnded(TicketDoneEndedListener listener) {
        doneEndedListeners.add(listener);
    }

    /** Tells {@code listener} whenever a session drew something. */
    public void onScreenChange(Runnable listener) {
        hosts.onScreenChange(id -> listener.run());
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
    private HostStart start(LaunchCommand command, WorkingDirectory directory, PaneSize size) {
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
        }
        StartedAt spawnedAt = new StartedAt(clock.clock().instant().truncatedTo(ChronoUnit.MILLIS));
        try {
            ProcessId pid = hosts.spawn(launch, code -> exited(id, code));
            return registered(HostedSession.builder()
                    .pid(pid)
                    .directory(directory)
                    .command(command)
                    .spawnedAt(spawnedAt)
                    .build());
        } catch (HostUnavailableException unavailable) {
            synchronized (open) {
                spawning.remove(id);
            }
            warnings.raise(new Warning(START_WARNING.formatted(unavailable.getMessage())));
            return new NotStartedHost();
        }
    }

    /** A claude that died before its spawn returned is never held: nothing would ever close its tab. */
    private HostStart registered(HostedSession session) {
        Optional<ExitCode> earlyExit;
        synchronized (open) {
            spawning.remove(session.id());
            earlyExit = Optional.ofNullable(earlyExits.remove(session.id()));
            if (earlyExit.isEmpty()) {
                open.add(session);
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

    private void exited(SessionId id, ExitCode code) {
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
        warnOfFailure(code);
        changed();
        dropped.flatMap(HostedSession::doneTicket).ifPresent(this::doneEnded);
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

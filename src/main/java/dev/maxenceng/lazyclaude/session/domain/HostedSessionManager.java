package dev.maxenceng.lazyclaude.session.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
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
    private final WarningPort warnings;
    private final WorktreePort worktrees;
    private final List<HostedSession> open = new ArrayList<>();
    private final List<HostedSessionId> spawning = new ArrayList<>();
    private final Map<HostedSessionId, ExitCode> earlyExits = new HashMap<>();
    private final List<OpenSessionsListener> changeListeners = new CopyOnWriteArrayList<>();
    private final List<TicketDoneEndedListener> doneEndedListeners = new CopyOnWriteArrayList<>();

    public HostedSessionManager(HostPort hosts, WarningPort warnings, WorktreePort worktrees) {
        this.hosts = hosts;
        this.warnings = warnings;
        this.worktrees = worktrees;
    }

    /** A plain {@code claude} in the project root. */
    public HostStart startUnlinked(Project project, PaneSize size) {
        return start(new NewClaude(), new WorkingDirectory(project.root()), size);
    }

    /**
     * {@code /ticket <verb> <ID>} in the ticket's worktree: where its branch is already checked out, else a worktree added at the
     * path the project keeps it at, on the branch when it exists and on a new one from {@code main} when not. None, with a warning,
     * when that path is taken by something else or git refuses.
     */
    public HostStart startTicket(TicketStart ticket, PaneSize size) {
        try {
            return start(
                    TicketClaude.builder().ticket(ticket.ticket()).verb(ticket.verb()).build(),
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
     * session already resuming it when there is one, since two {@code claude} on one transcript would both write to it.
     */
    public HostStart resume(SessionId session, WorkingDirectory recorded, PaneSize size) {
        Optional<HostedSession> resuming =
                open().sessions().stream().filter(hosted -> hosted.resumes(session)).findFirst();
        if (resuming.isPresent()) {
            return new StartedHost(resuming.get().id());
        }
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

    public void send(HostedSessionId id, TerminalInput input) {
        hosts.send(id, input);
    }

    public void resize(HostedSessionId id, PaneSize size) {
        hosts.resize(id, size);
    }

    public TerminalScreen screenOf(HostedSessionId id) {
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
        Optional<WorkingDirectory> held = worktrees.checkedOutAt(worktree.project(), worktree.branch());
        if (held.isPresent()) {
            return held.get();
        }
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

    private HostStart start(LaunchCommand command, WorkingDirectory directory, PaneSize size) {
        HostedSessionId id = HostedSessionId.random();
        HostLaunch launch = HostLaunch.builder()
                .id(id)
                .command(command)
                .directory(directory)
                .size(size)
                .build();
        synchronized (open) {
            spawning.add(id);
        }
        try {
            ProcessId pid = hosts.spawn(launch, code -> exited(id, code));
            return registered(HostedSession.builder()
                    .id(id)
                    .pid(pid)
                    .directory(directory)
                    .command(command)
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
        if (earlyExit.isPresent()) {
            warnOfFailure(earlyExit.get());
            return new NotStartedHost();
        }
        changed();
        return new StartedHost(session.id());
    }

    private void exited(HostedSessionId id, ExitCode code) {
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

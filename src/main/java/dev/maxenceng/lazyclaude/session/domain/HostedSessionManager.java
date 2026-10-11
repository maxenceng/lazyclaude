package dev.maxenceng.lazyclaude.session.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
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
 * project root, a resumed one in the directory its transcript recorded, or in the root with a warning when that directory
 * is gone. A session whose {@code claude} exits is dropped, with a warning when the exit was not clean; one that could not
 * be started is never held, and says why.
 */
public final class HostedSessionManager {

    private static final String START_WARNING = "hosted session: %s.";
    private static final String EXIT_WARNING = "hosted session: claude exited with code %d.";
    private static final String GONE_WARNING = "resume: %s is gone; running in %s instead.";

    private final HostPort hosts;
    private final WarningPort warnings;
    private final List<HostedSession> open = new ArrayList<>();
    private final List<HostedSessionId> spawning = new ArrayList<>();
    private final Map<HostedSessionId, ExitCode> earlyExits = new HashMap<>();
    private final List<OpenSessionsListener> changeListeners = new CopyOnWriteArrayList<>();

    public HostedSessionManager(HostPort hosts, WarningPort warnings) {
        this.hosts = hosts;
        this.warnings = warnings;
    }

    /** A plain {@code claude} in the project root. */
    public HostStart startUnlinked(Project project, PaneSize size) {
        return start(new NewClaude(), new WorkingDirectory(project.root()), size);
    }

    /** {@code claude --resume} of {@code session}, in {@code recorded} when it still exists, else in the project root. */
    public HostStart resume(Project project, SessionId session, WorkingDirectory recorded, PaneSize size) {
        WorkingDirectory root = new WorkingDirectory(project.root());
        if (hosts.exists(recorded)) {
            return start(new ResumeClaude(session), recorded, size);
        }
        warnings.raise(new Warning(GONE_WARNING.formatted(recorded.path(), root.path())));
        return start(new ResumeClaude(session), root, size);
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

    /** Tells {@code listener} whenever a session drew something. */
    public void onScreenChange(Runnable listener) {
        hosts.onScreenChange(id -> listener.run());
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
            return registered(HostedSession.builder().id(id).pid(pid).directory(directory).build());
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
        earlyExit.ifPresentOrElse(this::warnOfFailure, this::changed);
        return new StartedHost(session.id());
    }

    private void exited(HostedSessionId id, ExitCode code) {
        boolean dropped;
        synchronized (open) {
            dropped = open.removeIf(session -> session.id().equals(id));
            if (!dropped && spawning.contains(id)) {
                earlyExits.put(id, code);
            }
        }
        if (!dropped) {
            return;
        }
        warnOfFailure(code);
        changed();
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

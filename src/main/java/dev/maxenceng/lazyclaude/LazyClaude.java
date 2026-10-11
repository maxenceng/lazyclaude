package dev.maxenceng.lazyclaude;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogRepository;
import dev.maxenceng.lazyclaude.docs.application.DocsApplicationService;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsRepository;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchRepository;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeAgentsRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.LiveSessionRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionRegistryRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptCacheFile;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchRepository;
import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.StoppedThreadPayload;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningCommand;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQuery;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;
import dev.maxenceng.lazyclaude.shell.Shell;

/** Composition root: the one place that builds adapters and hands them to the shell. */
public final class LazyClaude {

    private static final String PREFIX = "lazyclaude: ";
    private static final String USER_HOME = "user.home";
    private static final String REGISTRY = ".claude/sessions";
    private static final String TRANSCRIPTS = ".claude/projects";
    private static final String XDG_CACHE_HOME = "XDG_CACHE_HOME";
    private static final String DEFAULT_CACHE_HOME = ".cache";
    private static final String CACHE_DIRECTORY = "lazyclaude";
    private static final String CACHE_FILE = "past-sessions.json";
    private static final Path PROC = Path.of("/proc");
    private static final Path MACHINE_ID = Path.of("/etc/machine-id");
    private static final List<String> CLAUDE_AGENTS = List.of("claude", "agents", "--json");
    private static final Duration CLAUDE_AGENTS_PATIENCE = Duration.ofSeconds(10);

    private LazyClaude() {}

    static void main() throws Exception {
        BoardQuery board = new BoardQuery(
                new BoardApplicationService(new ProjectLocatorRepository(), new BacklogRepository()));
        WarningRepository warningRepository = new WarningRepository();
        WarningApplicationService warningService = new WarningApplicationService(warningRepository, warningRepository);
        WarningQuery warnings = new WarningQuery(warningService);
        Thread.setDefaultUncaughtExceptionHandler(
                warnOfStoppedThreads(Thread.currentThread(), new WarningCommand(warningService), System.err));
        DocsQuery docs = new DocsQuery(
                new DocsApplicationService(new DocsRepository(), new DocsWatchRepository(), warningRepository));
        SessionQuery sessions = sessionQuery(warningRepository);
        int exit = start(
                Path.of("").toAbsolutePath(),
                board,
                docs,
                sessions,
                System.err,
                (view, tree, live) -> run(view, tree, live, docs, sessions, warnings));
        if (exit != 0) {
            System.exit(exit);
        }
    }

    /**
     * A thread other than {@code main} that ends on a failure nobody caught raises a warning, since the
     * screen is up and a stack trace would print over it. {@code main} carries the shell, so the screen
     * is gone by the time a failure reaches here, and it is printed as the JVM would.
     */
    static Thread.UncaughtExceptionHandler warnOfStoppedThreads(Thread main, WarningCommand warnings, PrintStream err) {
        return (thread, failure) -> {
            if (thread == main) {
                failure.printStackTrace(err);
            } else {
                warnings.threadStopped(new StoppedThreadPayload(thread.getName(), failure));
            }
        };
    }

    private static SessionQuery sessionQuery(WarningPort warnings) {
        Path home = Path.of(System.getProperty(USER_HOME));
        Path registry = home.resolve(REGISTRY);
        TranscriptRepository transcripts = new TranscriptRepository(
                home.resolve(TRANSCRIPTS),
                new TranscriptCacheFile(cacheHome(home, System.getenv(XDG_CACHE_HOME))
                        .resolve(CACHE_DIRECTORY)
                        .resolve(CACHE_FILE)));
        LiveSessionRepository live = new LiveSessionRepository(
                new SessionRegistryRepository(registry, PROC, MACHINE_ID),
                new ClaudeAgentsRepository(CLAUDE_AGENTS, CLAUDE_AGENTS_PATIENCE),
                SessionWatchRepository.SWEEP_INTERVAL);
        return new SessionQuery(new SessionApplicationService(
                live,
                transcripts,
                new ProjectLocatorRepository(),
                new SessionWatchRepository(registry),
                new TranscriptWatchRepository(home.resolve(TRANSCRIPTS), transcripts),
                warnings,
                Clock.systemDefaultZone()));
    }

    /** {@code $XDG_CACHE_HOME} when it is set to an absolute path, else {@code ~/.cache}, as the XDG base directory spec has it. */
    static Path cacheHome(Path home, String xdgCacheHome) {
        if (xdgCacheHome != null && !xdgCacheHome.isBlank() && Path.of(xdgCacheHome).isAbsolute()) {
            return Path.of(xdgCacheHome);
        }
        return home.resolve(DEFAULT_CACHE_HOME);
    }

    /**
     * Opens the shell on the board of the project {@code from} belongs to and that project's docs and
     * live and past sessions, or says why there is no board and returns non-zero without opening it. Docs that
     * cannot be listed and sessions that cannot be read do not stop it: their panels say why.
     */
    static int start(
            Path from, BoardQuery board, DocsQuery docs, SessionQuery sessions, PrintStream err, ShellOpener shell)
            throws Exception {
        BoardView view;
        DocsView tree;
        SessionsView live;
        try {
            view = board.boardFor(new BoardPayload(from));
            tree = docs.docsFor(new DocsPayload(view.root().value()));
            live = sessions.sessionsFor(sessionPayload(view));
        } catch (DomainException e) {
            err.println(PREFIX + e.getMessage());
            return 1;
        }
        shell.open(view, tree, live);
        return 0;
    }

    /** The project's root and the id of every ticket on its board, which {@code session} links its sessions against (ADR 0015). */
    static SessionPayload sessionPayload(BoardView view) {
        return new SessionPayload(
                view.root().value(),
                view.groups().stream()
                        .flatMap(group -> group.tickets().stream())
                        .map(ticket -> ticket.id().value())
                        .toList());
    }

    private static void run(
            BoardView view,
            DocsView tree,
            SessionsView live,
            DocsQuery docs,
            SessionQuery sessions,
            WarningQuery warnings)
            throws Exception {
        shellOn(view, tree, live, docs, sessions, warnings).run();
    }

    /**
     * The shell on {@code view}, {@code tree} and {@code live}, handed the project's docs and sessions again
     * after each change from now on, and every warning raised, those raised before it opened included.
     */
    // deferred: a change between the first read and the watch starting is shown only at the next change — close it when a doc edited during start-up is seen stale.
    static Shell shellOn(
            BoardView view,
            DocsView tree,
            SessionsView live,
            DocsQuery docs,
            SessionQuery sessions,
            WarningQuery warnings) {
        Shell shell = new Shell(view, tree, live);
        docs.subscribe(new DocsPayload(view.root().value()), shell::docsChanged);
        sessions.subscribe(sessionPayload(view), shell::sessionsChanged);
        warnings.subscribe(shell::warningsChanged);
        return shell;
    }
}

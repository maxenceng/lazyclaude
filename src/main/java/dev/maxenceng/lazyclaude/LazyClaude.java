package dev.maxenceng.lazyclaude;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardWatchPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.NoProjectView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnavailableBoardView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewPayload;
import dev.maxenceng.lazyclaude.session.application.ConfigurationApplicationService;
import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulators;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeKeybindingsRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigurationRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.LaunchLogRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.NotifySendRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionRegistryRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TimerRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptCacheFile;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.WorktreeRepository;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.StoppedThreadPayload;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningCommand;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;
import dev.maxenceng.lazyclaude.shell.Shell;
import dev.maxenceng.lazyclaude.sidecar.Sidecar;

/**
 * Composition root: the one place that builds adapters and hands them to the shell, or with {@code --sidecar} to the sidecar. With
 * {@code --protocol-schema} it builds nothing and prints the sidecar's schema.
 */
public final class LazyClaude {

    private static final String SIDECAR = "--sidecar";
    private static final String PROTOCOL_SCHEMA = "--protocol-schema";
    private static final String HERE = "";
    private static final String PREFIX = "lazyclaude: ";
    private static final String USER_HOME = "user.home";
    private static final String REGISTRY = ".claude/sessions";
    private static final String TRANSCRIPTS = ".claude/projects";
    private static final String XDG_CACHE_HOME = "XDG_CACHE_HOME";
    private static final String DEFAULT_CACHE_HOME = ".cache";
    private static final String XDG_STATE_HOME = "XDG_STATE_HOME";
    private static final String DEFAULT_STATE_HOME = ".local/state";
    private static final String CACHE_DIRECTORY = "lazyclaude";
    private static final String CACHE_FILE = "past-sessions.json";
    private static final List<String> CLAUDE = List.of("claude");
    private static final List<String> NOTIFY_SEND = List.of("notify-send");
    private static final List<String> GDBUS = List.of("gdbus");
    private static final Duration NOTIFY_PATIENCE = Duration.ofSeconds(5);
    private static final String REPAINT_THREAD = "session-repaint";

    private LazyClaude() {}

    static void main(String[] arguments) throws Exception {
        if (Arrays.asList(arguments).contains(PROTOCOL_SCHEMA)) {
            System.out.print(Sidecar.protocolSchema());
            return;
        }
        boolean sidecar = Arrays.asList(arguments).contains(SIDECAR);
        OutputStream protocol = System.out;
        if (sidecar) {
            System.setOut(System.err);
        }
        WarningRepository warningRepository = new WarningRepository();
        Path home = Path.of(System.getProperty(USER_HOME));
        int exit = configured(
                new ConfigurationQuery(new ConfigurationApplicationService(
                        new ConfigurationRepository(home, System.getenv()),
                        new ClaudeKeybindingsRepository(home),
                        warningRepository)),
                System.err,
                configuration -> {
                    SessionApplicationService sessionService = sessionService(warningRepository, configuration.desktopNotifications());
                    Contexts contexts = Contexts.of(warningRepository, sessionService);
                    if (sidecar) {
                        serve(protocol, contexts);
                    } else {
                        open(configuration, contexts, sessionService);
                    }
                });
        if (exit != 0) {
            System.exit(exit);
        }
    }

    /**
     * Answers the desktop frontend on stdin and {@code protocol}, the real stdout, until stdin closes (ADR 0050). {@code main} sends
     * {@code System.out} to stderr before anything is built, so nothing else, a library included, prints into the protocol.
     */
    private static void serve(OutputStream protocol, Contexts contexts) {
        Sidecar sidecar = new Sidecar(
                contexts.board(),
                contexts.sessions(),
                contexts.hosting(),
                contexts.warnings(),
                contexts.docs(),
                contexts.review(),
                contexts.pendingReview());
        System.exit(sidecar.run(System.in, protocol));
    }

    /** Opens the shell on {@code contexts}, with the terminal emulators of hosted sessions only the shell draws. */
    private static void open(ConfigurationView configuration, Contexts contexts, SessionApplicationService sessionService) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(warnOfStoppedThreads(Thread.currentThread(), contexts.warning(), System.err));
        HostedEmulators running = HostedEmulators.listeningTo(sessionService);
        HostedEmulatorCommand emulating = new HostedEmulatorCommand(sessionService, running);
        HostedEmulatorQuery emulators = new HostedEmulatorQuery(running);
        int exit = start(
                Path.of(HERE).toAbsolutePath(),
                contexts,
                System.err,
                (view, tree, live) -> runThenEndHosted(
                        shellOn(view, tree, live, contexts, emulating, emulators, configuration.reservedChords(), configuration.mouse())::run,
                        contexts.hosting()));
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

    /**
     * Opens the application on what the user configured, read before anything else so that a file that cannot be used stops
     * start-up before a frame is drawn: the reason is printed and the exit code is non-zero.
     */
    static int configured(ConfigurationQuery configuring, PrintStream err, ConfigurationOpener application) throws Exception {
        ConfigurationView configuration;
        try {
            configuration = configuring.configuration();
        } catch (DomainException e) {
            err.println(PREFIX + e.getMessage());
            return 1;
        }
        application.open(configuration);
        return 0;
    }

    private static SessionApplicationService sessionService(WarningPort warnings, boolean desktopNotifications) {
        Path home = Path.of(System.getProperty(USER_HOME));
        Path registry = home.resolve(REGISTRY);
        TranscriptRepository transcripts = new TranscriptRepository(
                home.resolve(TRANSCRIPTS),
                new TranscriptCacheFile(cacheHome(home, System.getenv(XDG_CACHE_HOME))
                        .resolve(CACHE_DIRECTORY)
                        .resolve(CACHE_FILE)));
        return new SessionApplicationService(
                new SessionRegistryRepository(registry),
                transcripts,
                new LaunchLogRepository(stateHome(home, System.getenv(XDG_STATE_HOME)).resolve(CACHE_DIRECTORY)),
                new SessionWatchRepository(registry),
                new TranscriptWatchRepository(home.resolve(TRANSCRIPTS), transcripts),
                warnings,
                new PtyHostRepository(CLAUDE, System.getenv()),
                new WorktreeRepository(),
                new NotifySendRepository(NOTIFY_SEND, GDBUS, NOTIFY_PATIENCE),
                desktopNotifications,
                new TimerRepository(Executors.newSingleThreadScheduledExecutor(task -> Thread.ofPlatform().name(REPAINT_THREAD).daemon().unstarted(task))),
                Clock.systemDefaultZone());
    }

    /** {@code $XDG_CACHE_HOME} when it is set to an absolute path, else {@code ~/.cache}, as the XDG base directory spec has it. */
    static Path cacheHome(Path home, String xdgCacheHome) {
        return Optional.ofNullable(xdgCacheHome)
                .filter(set -> !set.isBlank())
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElseGet(() -> home.resolve(DEFAULT_CACHE_HOME));
    }

    /** {@code $XDG_STATE_HOME} when it is set to an absolute path, else {@code ~/.local/state}, as the XDG base directory spec has it. */
    static Path stateHome(Path home, String xdgStateHome) {
        return Optional.ofNullable(xdgStateHome)
                .filter(set -> !set.isBlank())
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElseGet(() -> home.resolve(DEFAULT_STATE_HOME));
    }

    /**
     * Opens the shell on the board of the project {@code from} belongs to and that project's docs and
     * live and past sessions, or says why there is no project and returns non-zero without opening it. Docs that
     * cannot be listed and issues that cannot be read do not stop it: their panel says why.
     */
    static int start(Path from, Contexts contexts, PrintStream err, ShellOpener shell) throws Exception {
        BoardReadingView view;
        DocsView tree;
        SessionsView live;
        switch (contexts.board().boardFor(new BoardPayload(from))) {
            case NoProjectView none -> {
                err.println(PREFIX + none.reason().value());
                return 1;
            }
            case BoardReadingView read -> view = read;
        }
        try {
            tree = contexts.docs().docsFor(new DocsPayload(view.root().value()));
            live = contexts.sessions().sessionsFor(sessionPayload(view));
        } catch (DomainException e) {
            err.println(PREFIX + e.getMessage());
            return 1;
        }
        shell.open(view, tree, live);
        return 0;
    }

    /** The project's root and the id of every ticket on its board, which {@code session} links its sessions against (ADR 0015). */
    static SessionPayload sessionPayload(BoardReadingView view) {
        List<LinkedTicketId> ids = switch (view) {
            case BoardView read -> read.groups().stream()
                    .flatMap(group -> group.tickets().stream())
                    .map(ticket -> new LinkedTicketId(ticket.id().value()))
                    .toList();
            case UnavailableBoardView _ -> List.of();
        };
        return new SessionPayload(view.root().value(), ids);
    }

    /** Runs the shell, and however it ends, ends every hosted {@code claude}: they live and die with lazyclaude (ADR 0026). */
    static void runThenEndHosted(ShellRun shell, HostedSessionCommand hosting) throws Exception {
        try {
            shell.run();
        } finally {
            hosting.endAll();
        }
    }

    /**
     * The shell on {@code view}, {@code tree} and {@code live}, handed the project's docs and sessions again
     * after each change from now on, its open and merged pull requests as each fetch answers, the board read again when the backlog, a ticket worktree's copy of a ticket, the refs or the merged ticket branches change, a repaint whenever a hosted session draws, and every warning raised, those raised
     * before it opened included.
     */
    // deferred: a change between the first read and the watch starting is shown only at the next change — close it when a doc edited during start-up is seen stale.
    static Shell shellOn(
            BoardReadingView view,
            DocsView tree,
            SessionsView live,
            Contexts contexts,
            HostedEmulatorCommand emulating,
            HostedEmulatorQuery emulators,
            ReservedChords chords,
            boolean mouse) {
        Shell shell = new Shell(
                view,
                tree,
                live,
                contexts.review().checking(),
                contexts.hosting(),
                emulating,
                emulators,
                contexts.watching(),
                contexts.removing(),
                contexts.sessions(),
                contexts.board(),
                contexts.docs(),
                contexts.review(),
                contexts.pendingReview(),
                contexts.capturing(),
                chords,
                mouse);
        contexts.docs().subscribe(new DocsPayload(view.root().value()), shell::docsChanged);
        contexts.board().subscribe(new BoardWatchPayload(view.root().value()), shell::boardChanged);
        contexts.sessions().subscribe(sessionPayload(view), shell::sessionsChanged);
        emulators.onScreenChange(shell::screensChanged);
        contexts.sessions().onTicketDoneEnded(shell::ticketDoneEnded);
        contexts.review().subscribe(
                new ReviewPayload(view.root().value()), shell::pullRequestsChanged, shell::mergedPullRequestsChanged);
        contexts.warnings().subscribe(shell::warningsChanged);
        contexts.warnings().onNoneHeld(shell::warningsCleared);
        return shell;
    }
}

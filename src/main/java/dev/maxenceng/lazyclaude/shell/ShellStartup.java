package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.Contexts;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardWatchPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.NoProjectView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnavailableBoardView;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.StoppedThreadPayload;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningCommand;

/** How the TUI opens on the contexts and closes: what it reads before the first frame, what it watches, and what ends with it (ADR 0077). */
public final class ShellStartup {

    private ShellStartup() {}

    /**
     * Opens the shell on the project {@code from} belongs to with what the user configured, and returns once the user quits, or with
     * the reason there is no project to open. Only the shell draws hosted sessions, so only it asks their emulators to run. A failure
     * that ends {@code main} is handed to {@code mainFailed}, since the screen is gone by then.
     */
    public static Optional<Reason> open(Contexts contexts, ConfigurationView configuration, Path from, Consumer<Throwable> mainFailed)
            throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(warnOfStoppedThreads(Thread.currentThread(), contexts.warning(), mainFailed));
        contexts.emulating().emulate();
        return start(
                from,
                contexts,
                (view, tree, live) -> runThenEndHosted(
                        shellOn(view, tree, live, contexts, configuration.reservedChords(), configuration.mouse())::run,
                        contexts.hosting()));
    }

    /**
     * A thread other than {@code main} that ends on a failure nobody caught raises a warning, since the
     * screen is up and a stack trace would print over it. {@code main} carries the shell, so the screen
     * is gone by the time a failure reaches here, and it is handed to {@code mainFailed} to print as the JVM would.
     */
    static Thread.UncaughtExceptionHandler warnOfStoppedThreads(Thread main, WarningCommand warnings, Consumer<Throwable> mainFailed) {
        return (thread, failure) -> {
            if (thread == main) {
                mainFailed.accept(failure);
            } else {
                warnings.threadStopped(new StoppedThreadPayload(thread.getName(), failure));
            }
        };
    }

    /**
     * Opens the shell on the board of the project {@code from} belongs to and that project's docs and
     * live and past sessions, or returns why there is no project without opening it. Docs that
     * cannot be listed and issues that cannot be read do not stop it: their panel says why.
     */
    static Optional<Reason> start(Path from, Contexts contexts, ShellOpener shell) throws Exception {
        return switch (contexts.board().boardFor(new BoardPayload(from))) {
            case NoProjectView none -> Optional.of(none.reason());
            case BoardReadingView view -> {
                DocsView tree = contexts.docs().docsFor(new DocsPayload(view.root().value()));
                SessionsView live = contexts.sessions().sessionsFor(sessionPayload(view));
                shell.open(view, tree, live);
                yield Optional.empty();
            }
        };
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
            ReservedChords chords,
            boolean mouse) {
        Shell shell = new Shell(view, tree, live, contexts, chords, mouse);
        contexts.docs().subscribe(new DocsPayload(view.root().value()), shell::docsChanged);
        contexts.board().subscribe(new BoardWatchPayload(view.root().value()), shell::boardChanged);
        contexts.sessions().subscribe(sessionPayload(view), shell::sessionsChanged);
        contexts.emulators().onScreenChange(shell::screensChanged);
        contexts.sessions().onTicketDoneEnded(shell::ticketDoneEnded);
        contexts.review().subscribe(
                new ReviewPayload(view.root().value()), shell::pullRequestsChanged, shell::mergedPullRequestsChanged);
        contexts.warnings().subscribe(shell::warningsChanged);
        contexts.warnings().onNoneHeld(shell::warningsCleared);
        return shell;
    }
}

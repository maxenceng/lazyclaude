package dev.maxenceng.lazyclaude;

import java.io.PrintStream;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.docs.application.DocsApplicationService;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsRepository;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchRepository;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.shell.Shell;

/** Composition root: the one place that builds adapters and hands them to the shell. */
public final class LazyClaude {

    private static final String PREFIX = "lazyclaude: ";

    private LazyClaude() {}

    static void main() throws Exception {
        BoardQuery board = new BoardQuery(
                new BoardApplicationService(new ProjectLocatorRepository(), new BacklogRepository()));
        DocsQuery docs = new DocsQuery(
                new DocsApplicationService(new DocsRepository(), new DocsWatchRepository(System.err)));
        int exit = start(Path.of("").toAbsolutePath(), board, docs, System.err, (view, tree) -> run(view, tree, docs));
        if (exit != 0) {
            System.exit(exit);
        }
    }

    /**
     * Opens the shell on the board of the project {@code from} belongs to and that project's docs, or
     * says why there is no board or no docs to show and returns non-zero without opening it.
     */
    static int start(Path from, BoardQuery board, DocsQuery docs, PrintStream err, ShellOpener shell)
            throws Exception {
        BoardView view;
        DocsView tree;
        try {
            view = board.boardFor(new BoardPayload(from));
            tree = docs.docsFor(new DocsPayload(view.root().value()));
        } catch (DomainException e) {
            err.println(PREFIX + e.getMessage());
            return 1;
        }
        shell.open(view, tree);
        return 0;
    }

    private static void run(BoardView view, DocsView tree, DocsQuery docs) throws Exception {
        Shell shell = new Shell(view, tree);
        docs.subscribe(new DocsPayload(view.root().value()), shell::docsChanged);
        shell.run();
    }
}

package dev.maxenceng.lazyclaude;

import java.io.PrintStream;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.shell.Shell;

/** Composition root: the one place that builds adapters and hands them to the shell. */
public final class LazyClaude {

    private static final String PREFIX = "lazyclaude: ";

    private LazyClaude() {}

    static void main() throws Exception {
        BoardQuery board = new BoardQuery(
                new BoardApplicationService(new ProjectLocatorRepository(), new BacklogRepository()));
        int exit = start(Path.of("").toAbsolutePath(), board, System.err, view -> new Shell(view).run());
        if (exit != 0) {
            System.exit(exit);
        }
    }

    /**
     * Opens the shell on the board of the project {@code from} belongs to, or says why there is
     * none and returns non-zero without opening it.
     */
    static int start(Path from, BoardQuery board, PrintStream err, ShellOpener shell) throws Exception {
        BoardView view;
        try {
            view = board.boardFor(new BoardPayload(from));
        } catch (DomainException e) {
            err.println(PREFIX + e.getMessage());
            return 1;
        }
        shell.open(view);
        return 0;
    }
}

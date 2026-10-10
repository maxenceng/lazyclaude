package dev.maxenceng.lazyclaude;

import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.application.ReadBoard;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.ProjectLocator;
import dev.maxenceng.lazyclaude.board.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.GitProjectLocator;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.MarkdownBacklog;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shell.Shell;

/** Composition root: the one place that builds adapters and hands them to the shell. */
public final class LazyClaude {

    private LazyClaude() {}

    static void main() throws Exception {
        int exit = start(
                Path.of("").toAbsolutePath(),
                new GitProjectLocator(),
                new ReadBoard(new MarkdownBacklog()),
                System.err,
                board -> new Shell(board).run());
        if (exit != 0) {
            System.exit(exit);
        }
    }

    /**
     * Opens the shell on the board of the project {@code from} belongs to, or says why there is
     * none and returns non-zero without opening it.
     */
    static int start(Path from, ProjectLocator locator, ReadBoard readBoard, PrintStream err, ShellOpener shell)
            throws Exception {
        Project project;
        try {
            project = locator.locate(from);
        } catch (ProjectNotFoundException e) {
            err.println("lazyclaude: " + e.getMessage());
            return 1;
        }
        Board board;
        try {
            board = readBoard.read(project);
        } catch (UncheckedIOException e) {
            err.println("lazyclaude: " + e.getMessage());
            return 1;
        }
        shell.open(board);
        return 0;
    }

    @FunctionalInterface
    interface ShellOpener {
        void open(Board board) throws Exception;
    }
}

package dev.maxenceng.lazyclaude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.application.ReadBoard;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.GitProjectLocator;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class LazyClaudeTest {

    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final List<Board> opened = new ArrayList<>();

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellOutsideAGitRepository(@TempDir Path outside)
            throws Exception {
        int exit = LazyClaude.start(
                outside, new GitProjectLocator(), new ReadBoard(project -> Board.of(List.of(), List.of())),
                stream(), opened::add);

        assertThat(exit).as("a missing project is a failed start").isNotZero();
        assertThat(err.toString(StandardCharsets.UTF_8))
                .as("the reason reaches the terminal")
                .startsWith("lazyclaude: ")
                .contains("not inside a git repository");
        assertThat(opened).as("no frame is drawn before the reason").isEmpty();
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellWhenTheBacklogCannotBeRead() throws Exception {
        int exit = LazyClaude.start(
                Path.of("/work/app"), from -> new Project(from),
                new ReadBoard(project -> {
                    throw new UncheckedIOException("cannot list /work/app/docs/backlog", new IOException("denied"));
                }),
                stream(), opened::add);

        assertThat(exit).isNotZero();
        assertThat(err.toString(StandardCharsets.UTF_8))
                .as("an unlistable backlog is reported, not a stack trace")
                .startsWith("lazyclaude: cannot list /work/app/docs/backlog");
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldOpenTheShellOnTheBoardOfTheLocatedProject() throws Exception {
        Project project = new Project(Path.of("/work/app"));
        Board board = Board.of(List.of(), List.of());

        int exit = LazyClaude.start(
                Path.of("/work/app/src"), from -> project,
                new ReadBoard(asked -> asked.equals(project) ? board : null), stream(), opened::add);

        assertThat(exit).isZero();
        assertThat(opened).containsExactly(board);
        assertThat(err.size()).as("a found project prints nothing").isZero();
    }

    private PrintStream stream() {
        return new PrintStream(err, true, StandardCharsets.UTF_8);
    }
}

package dev.maxenceng.lazyclaude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class LazyClaudeTest {

    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final List<BoardView> opened = new ArrayList<>();

    private static BoardQuery board(ProjectLocatorPort locator, BacklogPort backlog) {
        return new BoardQuery(new BoardApplicationService(locator, backlog));
    }

    private int start(Path from, BoardQuery board) throws Exception {
        return LazyClaude.start(from, board, new PrintStream(err, true, StandardCharsets.UTF_8), opened::add);
    }

    private static void git(Path in, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", in.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
    }

    private String said() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellOutsideAGitRepository(@TempDir Path outside)
            throws Exception {
        int exit = start(outside, board(new ProjectLocatorRepository(), new BacklogRepository()));

        assertThat(exit).as("a missing project is a failed start").isNotZero();
        assertThat(said()).as("the reason reaches the terminal").startsWith("lazyclaude: ").contains("not inside a git repository");
        assertThat(opened).as("no frame is drawn before the reason").isEmpty();
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellWhenTheMainWorktreeHasNoBacklog(@TempDir Path repo)
            throws Exception {
        git(repo, "init", "-q");

        int exit = start(repo, board(new ProjectLocatorRepository(), new BacklogRepository()));

        assertThat(exit).isNotZero();
        assertThat(said())
                .startsWith("lazyclaude: the main worktree has no ")
                .contains(repo.toRealPath().resolve("docs/backlog").toString());
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldReadTheMainWorktreesBacklogEvenWhenStartedInALinkedWorktreeThatHasOne(@TempDir Path sandbox)
            throws Exception {
        Path repo = Files.createDirectories(sandbox.resolve("app")).toRealPath();
        git(repo, "init", "-q", "-b", "main");
        Files.createDirectories(repo.resolve("docs/backlog"));
        Files.writeString(repo.resolve("docs/backlog/A-1.md"), "---\nid: A-1\nstatus: todo\n---\n");
        git(repo, "add", ".");
        git(repo, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "init");
        Path worktree = sandbox.resolve("app.worktrees/A-1-ticket");
        git(repo, "worktree", "add", "-q", "-b", "A-1-ticket", worktree.toString());
        Files.delete(repo.resolve("docs/backlog/A-1.md"));
        Files.delete(repo.resolve("docs/backlog"));

        int exit = start(worktree.toRealPath(), board(new ProjectLocatorRepository(), new BacklogRepository()));

        assertThat(exit).as("the main worktree's backlog is the one that counts").isNotZero();
        assertThat(said()).contains(repo.resolve("docs/backlog").toString());
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellWhenTheBacklogCannotBeListed() throws Exception {
        Path backlog = Path.of("/work/app/docs/backlog");
        int exit = start(
                Path.of("/work/app"),
                board(from -> new Project(from.path()), project -> {
                    throw new BacklogUnavailableException(backlog, new IOException("denied"));
                }));

        assertThat(exit).isNotZero();
        assertThat(said())
                .as("an unlistable backlog is reported, not a stack trace")
                .startsWith("lazyclaude: cannot list /work/app/docs/backlog: denied");
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldOpenTheShellOnTheBoardOfTheLocatedProject() throws Exception {
        Project project = new Project(Path.of("/work/app"));

        int exit = start(
                Path.of("/work/app/src"),
                board(from -> project, asked -> asked.equals(project) ? List.of() : null));

        assertThat(exit).isZero();
        assertThat(opened).singleElement().satisfies(view -> assertThat(view.groups()).hasSize(5));
        assertThat(err.size()).as("a found project prints nothing").isZero();
    }
}

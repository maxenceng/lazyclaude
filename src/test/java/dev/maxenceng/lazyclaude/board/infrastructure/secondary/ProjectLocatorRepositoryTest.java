package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.WorkingDirectory;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class ProjectLocatorRepositoryTest {

    @TempDir
    Path sandbox;

    private Path repo;

    private final ProjectLocatorRepository locator = new ProjectLocatorRepository();

    @BeforeEach
    void createRepository() throws Exception {
        repo = Files.createDirectories(sandbox.resolve("app")).toRealPath();
        git(repo, "init", "-q", "-b", "main");
        Files.createDirectories(repo.resolve("docs/backlog"));
        Files.writeString(repo.resolve("docs/backlog/A-1.md"), "---\nid: A-1\nstatus: todo\n---\n");
        git(repo, "add", ".");
        git(repo, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "init");
    }

    private static void git(Path in, String... args) throws Exception {
        String[] command = new String[args.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = in.toString();
        System.arraycopy(args, 0, command, 3, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor()).as(output).isZero();
    }

    @Test
    void shouldLocateTheProjectFromItsRoot() {
        assertThat(locator.locate(new WorkingDirectory(repo))).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheProjectFromASubdirectory() throws IOException {
        Path sub = Files.createDirectories(repo.resolve("src/main"));

        assertThat(locator.locate(new WorkingDirectory(sub))).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheMainWorktreeFromALinkedWorktree() throws Exception {
        Path worktree = sandbox.resolve("app.worktrees/A-1-ticket");
        git(repo, "worktree", "add", "-q", "-b", "A-1-ticket", worktree.toString());

        assertThat(locator.locate(new WorkingDirectory(worktree.toRealPath()))).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheMainWorktreeFromASubdirectoryOfALinkedWorktree() throws Exception {
        Path worktree = sandbox.resolve("app.worktrees/A-1-ticket");
        git(repo, "worktree", "add", "-q", "-b", "A-1-ticket", worktree.toString());

        assertThat(locator.locate(new WorkingDirectory(worktree.resolve("docs").toRealPath()))).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheProjectFromADirectoryGivenThroughASymlink() throws Exception {
        Path link = Files.createSymbolicLink(sandbox.resolve("link"), repo);

        assertThat(locator.locate(new WorkingDirectory(link))).isEqualTo(new Project(repo));
    }

    @Test
    void shouldSayItIsNotAGitRepositoryOutsideOne() throws IOException {
        Path outside = Files.createDirectories(sandbox.resolve("plain"));

        assertThatThrownBy(() -> locator.locate(new WorkingDirectory(outside)))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("not inside a git repository")
                .hasMessageContaining(outside.toString());
    }

    @Test
    void shouldLocateARepositoryWithoutABacklog() throws Exception {
        Path other = Files.createDirectories(sandbox.resolve("bare-app"));
        git(other, "init", "-q", "-b", "main");

        assertThat(locator.locate(new WorkingDirectory(other)))
                .as("whether a project has a backlog is the board's question, not the locator's")
                .isEqualTo(new Project(other.toRealPath()));
    }

    @Test
    void shouldSayWhenTheDirectoryDoesNotExist() {
        Path missing = sandbox.resolve("gone");

        assertThatThrownBy(() -> locator.locate(new WorkingDirectory(missing)))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }
}

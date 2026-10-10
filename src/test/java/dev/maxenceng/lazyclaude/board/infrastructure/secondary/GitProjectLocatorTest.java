package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class GitProjectLocatorTest {

    @TempDir
    Path sandbox;

    private Path repo;

    private final GitProjectLocator locator = new GitProjectLocator();

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
        assertThat(locator.locate(repo)).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheProjectFromASubdirectory() throws IOException {
        Path sub = Files.createDirectories(repo.resolve("src/main"));

        assertThat(locator.locate(sub)).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheMainWorktreeFromALinkedWorktree() throws Exception {
        Path worktree = sandbox.resolve("app.worktrees/A-1-ticket");
        git(repo, "worktree", "add", "-q", "-b", "A-1-ticket", worktree.toString());

        assertThat(locator.locate(worktree.toRealPath())).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheMainWorktreeFromASubdirectoryOfALinkedWorktree() throws Exception {
        Path worktree = sandbox.resolve("app.worktrees/A-1-ticket");
        git(repo, "worktree", "add", "-q", "-b", "A-1-ticket", worktree.toString());

        assertThat(locator.locate(worktree.resolve("docs").toRealPath())).isEqualTo(new Project(repo));
    }

    @Test
    void shouldLocateTheProjectFromADirectoryGivenThroughASymlink() throws Exception {
        Path link = Files.createSymbolicLink(sandbox.resolve("link"), repo);

        assertThat(locator.locate(link)).isEqualTo(new Project(repo));
    }

    @Test
    void shouldSayItIsNotAGitRepositoryOutsideOne() throws IOException {
        Path outside = Files.createDirectories(sandbox.resolve("plain"));

        assertThatThrownBy(() -> locator.locate(outside))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("not inside a git repository")
                .hasMessageContaining(outside.toString());
    }

    @Test
    void shouldSayWhenARepositoryHasNoBacklog() throws Exception {
        Path other = Files.createDirectories(sandbox.resolve("bare-app"));
        git(other, "init", "-q", "-b", "main");

        assertThatThrownBy(() -> locator.locate(other))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(other.toRealPath().resolve("docs/backlog").toString());
    }

    @Test
    void shouldSayWhenALinkedWorktreeHasABacklogButTheMainWorktreeDoesNot() throws Exception {
        Path worktree = sandbox.resolve("app.worktrees/A-1-ticket");
        git(repo, "worktree", "add", "-q", "-b", "A-1-ticket", worktree.toString());
        deleteTree(repo.resolve("docs"));

        assertThatThrownBy(() -> locator.locate(worktree.toRealPath()))
                .as("the main worktree's backlog is the one that counts")
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(repo.resolve("docs/backlog").toString());
    }

    @Test
    void shouldSayWhenTheDirectoryDoesNotExist() {
        Path missing = sandbox.resolve("gone");

        assertThatThrownBy(() -> locator.locate(missing))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }

    private static void deleteTree(Path path) throws IOException {
        try (var paths = Files.walk(path)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}

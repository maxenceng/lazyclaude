package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.linkedWorktree;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.missingGit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithACommit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithoutACommit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.ProjectUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

class ProjectLocatorRepositoryTest {

    private final ProjectLocatorRepository locator = new ProjectLocatorRepository();

    @TempDir
    Path sandbox;

    @Test
    void shouldLocateTheProjectFromItsRoot() throws Exception {
        Path repository = repositoryWithACommit(sandbox.resolve("app"));

        assertThat(locator.locate(new WorkingDirectory(repository))).isEqualTo(new Project(repository));
    }

    @Test
    void shouldLocateTheProjectFromASubdirectory() throws Exception {
        Path repository = repositoryWithACommit(sandbox.resolve("app"));

        assertThat(locator.locate(new WorkingDirectory(Files.createDirectories(repository.resolve("src/main")))))
                .isEqualTo(new Project(repository));
    }

    @Test
    void shouldLocateTheMainWorktreeFromALinkedWorktree() throws Exception {
        Path repository = repositoryWithACommit(sandbox.resolve("app"));
        Path worktree = linkedWorktree(repository, sandbox.resolve("app.worktrees/PROJ-1-ticket"));

        assertThat(locator.locate(new WorkingDirectory(worktree))).isEqualTo(new Project(repository));
    }

    @Test
    void shouldLocateTheMainWorktreeFromASubdirectoryOfALinkedWorktree() throws Exception {
        Path repository = repositoryWithACommit(sandbox.resolve("app"));
        Path worktree = linkedWorktree(repository, sandbox.resolve("app.worktrees/PROJ-1-ticket"));

        assertThat(locator.locate(new WorkingDirectory(worktree.resolve("docs")))).isEqualTo(new Project(repository));
    }

    @Test
    void shouldLocateTheProjectFromADirectoryGivenThroughASymlink() throws Exception {
        Path repository = repositoryWithACommit(sandbox.resolve("app"));
        Path link = Files.createSymbolicLink(sandbox.resolve("link"), repository);

        assertThat(locator.locate(new WorkingDirectory(link))).isEqualTo(new Project(repository));
    }

    @Test
    void shouldLocateARepositoryWithoutABacklog() throws Exception {
        Path repository = repositoryWithoutACommit(sandbox.resolve("bare-app"));

        assertThat(locator.locate(new WorkingDirectory(repository)))
                .as("whether a project has a backlog is the board's question, not the locator's")
                .isEqualTo(new Project(repository));
    }

    @Test
    void shouldSayItIsNotAGitRepositoryOutsideOne() throws Exception {
        Path outside = Files.createDirectories(sandbox.resolve("plain"));

        assertThatThrownBy(() -> locator.locate(new WorkingDirectory(outside)))
                .isExactlyInstanceOf(ProjectNotFoundException.class)
                .hasMessage("%s is not inside a git repository".formatted(outside));
    }

    @Test
    void shouldSayWhenTheDirectoryDoesNotExist() {
        Path missing = sandbox.resolve("gone");

        assertThatThrownBy(() -> locator.locate(new WorkingDirectory(missing)))
                .isExactlyInstanceOf(ProjectNotFoundException.class)
                .hasMessage("%s is not a directory".formatted(missing));
    }

    @Test
    void shouldSayTheLookupFailedRatherThanThatThereIsNoProjectWhenGitCannotRun() {
        assertThatThrownBy(() -> new ProjectLocatorRepository(missingGit()).locate(new WorkingDirectory(sandbox)))
                .as("a git that cannot run says nothing about whether the directory is in a repository")
                .isExactlyInstanceOf(ProjectUnavailableException.class)
                .hasMessageContaining(missingGit());
    }
}

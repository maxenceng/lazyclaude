package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.otherProject;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.otherProjectWorktreeDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.outsideGitDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectoryAt;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.RelativePathException;

class ProjectTest {

    @Test
    void shouldRefuseARelativeRoot() {
        assertThatThrownBy(() -> new Project(Path.of("app")))
                .as("two relative roots from different directories would compare equal")
                .isInstanceOf(RelativePathException.class);
    }

    @Test
    void shouldKeepATicketWorktreeBesideTheRootInAFolderNamedAfterTheRepository() {
        assertThat(project().ticketWorktree(branchName()))
                .as("the path rule is ../<repo>.worktrees/<ticket branch>")
                .contains(worktreeDirectory());
    }

    @Test
    void shouldHaveNoTicketWorktreeWhenTheRootIsTheFilesystemRoot() {
        assertThat(new Project(Path.of("/")).ticketWorktree(branchName()))
                .as("a root with no name has no sibling folder to keep worktrees in")
                .isEmpty();
    }

    @Test
    void shouldOwnATicketWorktreeByTheFolderNamedAfterItsRepository() {
        assertThat(Project.owningTicketWorktree(otherProjectWorktreeDirectory()))
                .as("../<repo>.worktrees/<ticket branch> belongs to ../<repo>, read back from the path alone")
                .contains(otherProject());
    }

    @Test
    void shouldOwnNoDirectoryOutsideAWorktreesFolder() {
        assertThat(Project.owningTicketWorktree(outsideGitDirectory())).isEmpty();
    }

    @Test
    void shouldOwnNeitherTheWorktreesFolderItselfNorOneNamedOnlyTheSuffix() {
        assertThat(Project.owningTicketWorktree(workingDirectoryAt("/work/other.worktrees")))
                .as("the folder holds worktrees; it is none itself")
                .isEmpty();
        assertThat(Project.owningTicketWorktree(workingDirectoryAt("/work/.worktrees/PROJ-1-ticket")))
                .as("a folder named only the suffix names no repository")
                .isEmpty();
        assertThat(Project.owningTicketWorktree(workingDirectoryAt("/work/other.worktreesX/PROJ-1-ticket"))).isEmpty();
    }
}

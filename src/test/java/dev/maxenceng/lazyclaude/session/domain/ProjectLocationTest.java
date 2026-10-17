package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.otherProject;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.otherProjectWorktreeDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.outsideGitDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectoryAt;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.shared.domain.Project;

class ProjectLocationTest {

    @Test
    void shouldHoldWhatGitLocatedInTheProject() {
        assertThat(new LocatedInProject(project()).holds(project(), workingDirectoryAt("/elsewhere"))).isTrue();
    }

    @Test
    void shouldNotHoldWhatGitLocatedInAnotherProject() {
        assertThat(new LocatedInProject(new Project(Path.of("/work/other"))).holds(project(), workingDirectoryAt("/work/app")))
                .as("git's answer wins over a path that happens to lie under the project")
                .isFalse();
    }

    @Test
    void shouldHoldAPathUnderTheProjectRootWhenGitLocatedNothing() {
        assertThat(new NotLocated().holds(project(), workingDirectoryAt("/work/app/src/deleted"))).isTrue();
    }

    @Test
    void shouldHoldAPathUnderTheProjectsWorktreesFolderWhenGitLocatedNothing() {
        assertThat(new NotLocated().holds(project(), workingDirectoryAt("/work/app.worktrees/PROJ-1-ticket"))).isTrue();
    }

    @Test
    void shouldNotHoldAPathUnderAnotherProjectsWorktreesFolder() {
        assertThat(new NotLocated().holds(project(), workingDirectoryAt("/work/other.worktrees/PROJ-1-ticket"))).isFalse();
    }

    @Test
    void shouldNotHoldASiblingWhoseNameOnlyStartsLikeTheProject() {
        assertThat(new NotLocated().holds(project(), workingDirectoryAt("/work/app-other/src")))
                .as("the rule matches path segments, not name prefixes")
                .isFalse();
        assertThat(new NotLocated().holds(project(), workingDirectoryAt("/work/app.worktreesX/PROJ-1"))).isFalse();
    }

    @Test
    void shouldNotHoldAPathOutsideTheProject() {
        assertThat(new NotLocated().holds(project(), workingDirectoryAt("/tmp/scratch"))).isFalse();
    }

    @Test
    void shouldHoldEveryPathForAProjectAtTheFilesystemRootWithNoName() {
        assertThat(new NotLocated().holds(new Project(Path.of("/")), workingDirectoryAt("/tmp/scratch")))
                .as("the root has no folder name to derive a worktrees folder from, and everything is under it")
                .isTrue();
    }

    @Test
    void shouldPlaceAnotherProjectsRemovedWorktreeUnderThatProjectWhenGitLocatedNothing() {
        assertThat(new NotLocated().placeOf(project(), otherProjectWorktreeDirectory()))
                .as("a removed worktree of another project is that project's, not a place outside git")
                .isEqualTo(new ProjectPlace(otherProject()));
    }

    @Test
    void shouldPlaceADirectoryInNoProjectAsOutsideGit() {
        assertThat(new NotLocated().placeOf(project(), outsideGitDirectory()))
                .isEqualTo(new OutsideGitPlace(outsideGitDirectory()));
    }
}

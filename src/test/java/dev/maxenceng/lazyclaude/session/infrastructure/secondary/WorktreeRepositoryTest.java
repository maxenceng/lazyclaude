package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.secondBranchName;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchAtHead;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.linkedWorktreeOn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.missingGit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.removedWorktreeOn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithBacklog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

class WorktreeRepositoryTest {

    private final WorktreeRepository repository = new WorktreeRepository();

    @TempDir
    Path temp;

    private Path root;
    private Project project;

    @BeforeEach
    void createRepository() throws Exception {
        root = repositoryWithBacklog(temp.resolve("app"));
        project = new Project(root);
    }

    @Test
    void shouldFindTheRootCheckoutWhereMainIsCheckedOut() {
        assertThat(repository.checkedOutAt(project, new BranchName("main")))
                .contains(new WorkingDirectory(root));
    }

    @Test
    void shouldFindALinkedWorktreeWhereItsBranchIsCheckedOut() throws Exception {
        Path linked = linkedWorktreeOn(root, temp.resolve("app.worktrees").resolve(branchName().value()), branchName().value());

        assertThat(repository.checkedOutAt(project, branchName())).contains(new WorkingDirectory(linked));
    }

    @Test
    void shouldFindNothingForABranchCheckedOutNowhere() throws Exception {
        branchAtHead(root, branchName().value());

        assertThat(repository.checkedOutAt(project, branchName())).isEmpty();
    }

    @Test
    void shouldFindNothingForABranchWhoseWorktreeDirectoryIsGone() throws Exception {
        removedWorktreeOn(root, temp.resolve("app.worktrees").resolve(branchName().value()), branchName().value());

        assertThat(repository.checkedOutAt(project, branchName()))
                .as("a worktree git still lists but whose directory was deleted is no place to run in")
                .isEmpty();
    }

    @Test
    void shouldSayABranchExists() throws Exception {
        branchAtHead(root, branchName().value());

        assertThat(repository.branchExists(project, branchName())).isTrue();
    }

    @Test
    void shouldSayABranchDoesNotExist() {
        assertThat(repository.branchExists(project, secondBranchName())).isFalse();
    }

    @Test
    void shouldAddAWorktreeOnAnExistingBranch() throws Exception {
        branchAtHead(root, branchName().value());
        WorkingDirectory at = new WorkingDirectory(temp.resolve("app.worktrees").resolve(branchName().value()));

        repository.addOnBranch(project, branchName(), at);

        assertThat(repository.checkedOutAt(project, branchName())).contains(new WorkingDirectory(at.path().toRealPath()));
    }

    @Test
    void shouldAddAWorktreeOnANewBranchFromMain() {
        WorkingDirectory at = new WorkingDirectory(temp.resolve("app.worktrees").resolve(secondBranchName().value()));

        repository.addOnNewBranch(project, secondBranchName(), at);

        assertThat(repository.branchExists(project, secondBranchName())).isTrue();
        assertThat(at.path().resolve("docs").resolve("backlog").resolve("PROJ-1.md"))
                .as("the new branch starts from main, so its checkout holds main's files")
                .exists();
    }

    @Test
    void shouldAddAWorktreeBackOnItsBranchWhereGitStillListsTheDeletedOne() throws Exception {
        Path gone = temp.resolve("app.worktrees").resolve(branchName().value());
        removedWorktreeOn(root, gone, branchName().value());

        repository.addOnBranch(project, branchName(), new WorkingDirectory(gone));

        assertThat(repository.checkedOutAt(project, branchName()))
                .as("a worktree deleted without git worktree remove is still listed, and must not block adding it back")
                .contains(new WorkingDirectory(gone.toRealPath()));
    }

    @Test
    void shouldAddAWorktreeOnANewBranchWhereGitStillListsADeletedOne() throws Exception {
        Path gone = temp.resolve("app.worktrees").resolve(secondBranchName().value());
        removedWorktreeOn(root, gone, branchName().value());

        repository.addOnNewBranch(project, secondBranchName(), new WorkingDirectory(gone));

        assertThat(repository.checkedOutAt(project, secondBranchName()))
                .contains(new WorkingDirectory(gone.toRealPath()));
    }

    @Test
    void shouldRefuseToAddAWorktreeOnABranchCheckedOutElsewhere() throws Exception {
        branchAtHead(root, branchName().value());
        repository.addOnBranch(project, branchName(), new WorkingDirectory(temp.resolve("elsewhere")));

        assertThatThrownBy(() -> repository.addOnBranch(
                        project, branchName(), new WorkingDirectory(temp.resolve("app.worktrees").resolve("second"))))
                .isInstanceOf(WorktreeUnavailableException.class)
                .hasMessageContaining("already");
    }

    @Test
    void shouldFailWhenGitCannotBeRun() {
        assertThatThrownBy(() -> new WorktreeRepository(missingGit()).branchExists(project, branchName()))
                .isInstanceOf(WorktreeUnavailableException.class);
    }

    @Test
    void shouldFailToListWhenTheRootIsNotARepository() throws Exception {
        Path plain = Files.createDirectories(temp.resolve("plain"));

        assertThatThrownBy(() -> repository.checkedOutAt(new Project(plain), branchName()))
                .isInstanceOf(WorktreeUnavailableException.class);
    }
}

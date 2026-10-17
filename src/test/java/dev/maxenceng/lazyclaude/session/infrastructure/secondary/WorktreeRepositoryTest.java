package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.WorktreeRepositoryFixture.ticketWorktreePath;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.secondBranchName;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchAtHead;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchExistsIn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.commitEverything;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.headOf;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.linkedWorktreeOn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.missingGit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.pushHeadAs;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.remoteBeside;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.removedWorktreeOn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithBacklog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.review.published.HeadCommit;
import dev.maxenceng.lazyclaude.session.domain.CommitId;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotRemovedException;
import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;
import dev.maxenceng.lazyclaude.session.published.DirtyPath;
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

    @Test
    void shouldFindAListedWorktreeAtItsPath() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        assertThat(repository.isWorktreeAt(project, new WorkingDirectory(linked))).isTrue();
    }

    @Test
    void shouldNotFindAWorktreeAtADirectoryGitDoesNotList() throws Exception {
        Path plain = Files.createDirectories(ticketWorktreePath(temp));

        assertThat(repository.isWorktreeAt(project, new WorkingDirectory(plain)))
                .as("a folder at the planned path that is not a worktree is nothing to remove")
                .isFalse();
    }

    @Test
    void shouldNotFindAWorktreeWhoseDirectoryIsGone() throws Exception {
        Path gone = removedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        assertThat(repository.isWorktreeAt(project, new WorkingDirectory(gone))).isFalse();
    }

    @Test
    void shouldFailToListWhereGitCannotBeRunForAWorktreeAt() {
        assertThatThrownBy(() -> new WorktreeRepository(missingGit()).isWorktreeAt(project, new WorkingDirectory(root)))
                .isInstanceOf(WorktreeUnavailableException.class);
    }

    @Test
    void shouldFindNoDirtyPathInACleanWorktree() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        assertThat(repository.dirtyPaths(new WorkingDirectory(linked))).isEmpty();
    }

    @Test
    void shouldFindExactlyOneDirtyPathForOneModifiedFile() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        Files.writeString(linked.resolve("docs/backlog/PROJ-1.md"), "changed\n");

        assertThat(repository.dirtyPaths(new WorkingDirectory(linked)))
                .containsExactly(new DirtyPath("docs/backlog/PROJ-1.md"));
    }

    @Test
    void shouldFindAnUntrackedFileDirty() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        Files.writeString(linked.resolve("notes.txt"), "scratch\n");

        assertThat(repository.dirtyPaths(new WorkingDirectory(linked))).containsExactly(new DirtyPath("notes.txt"));
    }

    @Test
    void shouldListEachUntrackedFileOfAnUntrackedFolder() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        Files.createDirectories(linked.resolve("scratch"));
        Files.writeString(linked.resolve("scratch/a.txt"), "a\n");
        Files.writeString(linked.resolve("scratch/b.txt"), "b\n");

        assertThat(repository.dirtyPaths(new WorkingDirectory(linked)))
                .as("the paths are listed for the person to see what would be lost, not the folder that holds them")
                .containsExactlyInAnyOrder(new DirtyPath("scratch/a.txt"), new DirtyPath("scratch/b.txt"));
    }

    @Test
    void shouldFailToReadDirtyPathsWhereGitCannotBeRun() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        assertThatThrownBy(() -> new WorktreeRepository(missingGit()).dirtyPaths(new WorkingDirectory(linked)))
                .isInstanceOf(WorktreeUnavailableException.class);
    }

    @Test
    void shouldFailToReadDirtyPathsOfADirectoryThatIsNoRepository() throws Exception {
        Path plain = Files.createDirectories(temp.resolve("plain"));

        assertThatThrownBy(() -> repository.dirtyPaths(new WorkingDirectory(plain)))
                .isInstanceOf(WorktreeUnavailableException.class);
    }

    @Test
    void shouldFindNoCommitBeyondTheMergedHeadWhenNoRemoteRefHasIt() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        commitEverything(linked, "the ticket");

        assertThat(repository.commitsBeyond(new WorkingDirectory(linked), new HeadCommit(headOf(linked))))
                .as("a squash-merged ticket whose head branch was deleted has no remote ref, yet nothing is beyond the merge")
                .isEmpty();
    }

    @Test
    void shouldFindACommitBeyondTheMergedHeadThatNoRemoteRefHas() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        commitEverything(linked, "the ticket");
        HeadCommit merged = new HeadCommit(headOf(linked));
        commitEverything(linked, "after the merge");

        assertThat(repository.commitsBeyond(new WorkingDirectory(linked), merged))
                .containsExactly(new CommitId(headOf(linked)));
    }

    @Test
    void shouldFindNoCommitBeyondTheMergedHeadThatARemoteRefHas() throws Exception {
        remoteBeside(root, temp.resolve("remote.git"));
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        commitEverything(linked, "the ticket");
        HeadCommit merged = new HeadCommit(headOf(linked));
        commitEverything(linked, "the close-out");
        pushHeadAs(linked, "PROJ-1-close-out");

        assertThat(repository.commitsBeyond(new WorkingDirectory(linked), merged))
                .as("an open close-out pull request's commits are pushed, so they do not block")
                .isEmpty();
    }

    @Test
    void shouldFindOnlyTheCommitsAfterTheOnesARemoteRefHas() throws Exception {
        remoteBeside(root, temp.resolve("remote.git"));
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        commitEverything(linked, "the ticket");
        HeadCommit merged = new HeadCommit(headOf(linked));
        commitEverything(linked, "pushed");
        pushHeadAs(linked, "PROJ-1-close-out");
        commitEverything(linked, "not pushed");

        assertThat(repository.commitsBeyond(new WorkingDirectory(linked), merged))
                .containsExactly(new CommitId(headOf(linked)));
    }

    @Test
    void shouldFailToReadCommitsWhenTheMergedHeadIsUnknownToTheRepository() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        assertThatThrownBy(() -> repository.commitsBeyond(new WorkingDirectory(linked), new HeadCommit("1".repeat(40))))
                .isInstanceOf(WorktreeUnavailableException.class);
    }

    @Test
    void shouldNotTakeAMergedHeadForAnOptionOfGit() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        assertThatThrownBy(() -> repository.commitsBeyond(new WorkingDirectory(linked), new HeadCommit("--all")))
                .as("a head that begins with a dash is a revision git cannot find, never an option it obeys")
                .isInstanceOf(WorktreeUnavailableException.class);
    }

    @Test
    void shouldRemoveAWorktreeAndKeepItsBranch() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        repository.remove(project, new WorkingDirectory(linked));

        assertThat(linked).doesNotExist();
        assertThat(branchExistsIn(root, branchName().value())).as("deleting the branch is lazygit's job").isTrue();
        assertThat(repository.isWorktreeAt(project, new WorkingDirectory(linked))).isFalse();
    }

    @Test
    void shouldNotRemoveAWorktreeGitRefusesAndSayWhy() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());
        Files.writeString(linked.resolve("notes.txt"), "scratch\n");

        assertThatThrownBy(() -> repository.remove(project, new WorkingDirectory(linked)))
                .isInstanceOf(WorktreeNotRemovedException.class)
                .hasMessageContaining("untracked");
        assertThat(linked.resolve("notes.txt")).as("nothing is forced").exists();
    }

    @Test
    void shouldFailToRemoveWhereGitCannotBeRun() throws Exception {
        Path linked = linkedWorktreeOn(root, ticketWorktreePath(temp), branchName().value());

        assertThatThrownBy(() -> new WorktreeRepository(missingGit()).remove(project, new WorkingDirectory(linked)))
                .isInstanceOf(WorktreeNotRemovedException.class);
    }
}

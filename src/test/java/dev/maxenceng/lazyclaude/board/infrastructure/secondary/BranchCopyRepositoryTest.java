package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.CheckedOutBranchFixture.checkedOut;
import static dev.maxenceng.lazyclaude.board.domain.CheckedOutBranchFixture.trunk;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.backlogIn;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.ticketMarkdown;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.fileName;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.slugTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchAtHead;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchPointingNowhere;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchWithBacklogFile;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.detachedWorktree;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.linkedWorktreeOn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.missingGit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.removedWorktreeOn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithBacklog;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.tag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.DetachedHead;
import dev.maxenceng.lazyclaude.board.domain.TicketFile;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.domain.Worktree;
import dev.maxenceng.lazyclaude.board.domain.WorktreePath;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class BranchCopyRepositoryTest {

    private static final String IN_REVIEW = ticketMarkdown().replace("status: todo", "status: in-review");

    private final BranchCopyRepository repository = new BranchCopyRepository();

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
    void shouldListTheRootCheckoutWithItsBranch() {
        assertThat(repository.worktrees(project))
                .containsExactly(Worktree.builder().path(new WorktreePath(root)).head(trunk()).build());
    }

    @Test
    void shouldListALinkedWorktreeWithItsBranchAfterTheRootCheckout() throws Exception {
        Path linked = linkedWorktreeOn(root, temp.resolve("app.worktrees").resolve("PROJ-1-first-slice"), "PROJ-1-first-slice");

        assertThat(repository.worktrees(project))
                .containsExactly(
                        Worktree.builder().path(new WorktreePath(root)).head(trunk()).build(),
                        Worktree.builder().path(new WorktreePath(linked)).head(checkedOut(slugTicketBranch())).build());
    }

    @Test
    void shouldListADetachedWorktreeWithNoBranch() throws Exception {
        Path detached = detachedWorktree(root, temp.resolve("app.detached"));

        assertThat(repository.worktrees(project))
                .contains(Worktree.builder().path(new WorktreePath(detached)).head(new DetachedHead()).build());
    }

    @Test
    void shouldLeaveOutAWorktreeWhoseDirectoryIsGone() throws Exception {
        removedWorktreeOn(root, temp.resolve("app.worktrees").resolve("PROJ-1-first-slice"), "PROJ-1-first-slice");

        assertThat(repository.worktrees(project))
                .as("a worktree git still lists but whose directory was deleted holds no copy, so the branch is read instead")
                .containsExactly(Worktree.builder().path(new WorktreePath(root)).head(trunk()).build());
    }

    @Test
    void shouldReadTheFileOfAWorktreeWithItsUncommittedEdit() throws Exception {
        Files.writeString(backlogIn(root).resolve("PROJ-1.md"), IN_REVIEW);

        Optional<BacklogEntry> read =
                repository.inWorktree(Worktree.builder().path(new WorktreePath(root)).head(new DetachedHead()).build(), fileName());

        assertThat(read)
                .containsInstanceOf(TicketFile.class)
                .get()
                .extracting(entry -> ((TicketFile) entry).ticket().status())
                .isEqualTo(Status.IN_REVIEW);
    }

    @Test
    void shouldReadNothingFromAWorktreeThatLacksTheFile() throws Exception {
        Path linked = linkedWorktreeOn(root, temp.resolve("app.worktrees").resolve("PROJ-2"), "PROJ-2");
        Files.delete(linked.resolve("docs").resolve("backlog").resolve("PROJ-1.md"));

        assertThat(repository.inWorktree(
                        Worktree.builder().path(new WorktreePath(linked)).head(new DetachedHead()).build(), fileName()))
                .isEmpty();
    }

    @Test
    void shouldReadARefusedWorktreeFileAsUnreadable() throws Exception {
        Files.writeString(backlogIn(root).resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: blocked\n---\n");

        assertThat(repository.inWorktree(Worktree.builder().path(new WorktreePath(root)).head(new DetachedHead()).build(), fileName()))
                .contains(UnreadableTicket.builder()
                        .file(fileName())
                        .reason(new Reason("status 'blocked' is not one of draft, todo, in-progress, in-review, done"))
                        .build());
    }

    @Test
    void shouldReadTheFileAsABranchCommitsItWithoutCheckingItOut() throws Exception {
        branchWithBacklogFile(root, "PROJ-1", "PROJ-1.md", IN_REVIEW);

        assertThat(repository.onBranch(project, ticketBranch(), fileName()))
                .containsInstanceOf(TicketFile.class)
                .get()
                .extracting(entry -> ((TicketFile) entry).ticket().status())
                .isEqualTo(Status.IN_REVIEW);
        assertThat(Files.readString(backlogIn(root).resolve("PROJ-1.md"))).isEqualTo(ticketMarkdown());
    }

    @Test
    void shouldReadTheWholeTicketFromTheBranch() throws Exception {
        branchWithBacklogFile(root, "PROJ-1", "PROJ-1.md", IN_REVIEW);

        assertThat(repository.onBranch(project, ticketBranch(), fileName()))
                .contains(TicketFile.builder()
                        .file(fileName())
                        .ticket(ticketBuilder().status(Status.IN_REVIEW).build())
                        .build());
    }

    @Test
    void shouldReadNothingFromABranchThatDoesNotExist() {
        assertThat(repository.onBranch(project, ticketBranch(), fileName())).isEmpty();
    }

    @Test
    void shouldReadNothingFromABranchThatLacksTheFile() throws Exception {
        branchAtHead(root, "PROJ-1");

        assertThat(repository.onBranch(project, ticketBranch(), new FileName("PROJ-9.md"))).isEmpty();
    }

    @Test
    void shouldReadARefusedBranchFileAsUnreadable() throws Exception {
        branchWithBacklogFile(root, "PROJ-1", "PROJ-1.md", "---\nid: PROJ-1\nstatus: blocked\n---\n");

        assertThat(repository.onBranch(project, ticketBranch(), fileName()))
                .contains(UnreadableTicket.builder()
                        .file(fileName())
                        .reason(new Reason("status 'blocked' is not one of draft, todo, in-progress, in-review, done"))
                        .build());
    }

    @Test
    void shouldNotTakeATagForTheBranch() throws Exception {
        branchWithBacklogFile(root, "elsewhere", "PROJ-1.md", IN_REVIEW);
        tag(root, "PROJ-1", "elsewhere");

        assertThat(repository.onBranch(project, ticketBranch(), fileName()))
                .as("a tag named like the ticket branch is not the branch")
                .isEmpty();
    }

    @Test
    void shouldBeUnavailableWhenTheBranchExistsAndGitStillCannotGiveTheFile() throws Exception {
        branchPointingNowhere(root, "PROJ-1");

        assertThatThrownBy(() -> repository.onBranch(project, ticketBranch(), fileName()))
                .isInstanceOf(BranchCopyUnavailableException.class)
                .hasMessageContaining("git show")
                .hasMessageContaining("PROJ-1");
    }

    @Test
    void shouldBeUnavailableWhenGitCannotBeRun() {
        BranchCopyRepository withoutGit = new BranchCopyRepository(missingGit());

        assertThatThrownBy(() -> withoutGit.worktrees(project))
                .isInstanceOf(BranchCopyUnavailableException.class)
                .hasMessageContaining(missingGit());
        assertThatThrownBy(() -> withoutGit.onBranch(project, ticketBranch(), fileName()))
                .isInstanceOf(BranchCopyUnavailableException.class)
                .hasMessageContaining(missingGit());
    }

    @Test
    void shouldBeUnavailableWhenTheDirectoryIsNotARepository() throws Exception {
        Path plain = Files.createDirectories(temp.resolve("plain"));

        assertThatThrownBy(() -> repository.worktrees(new Project(plain)))
                .isInstanceOf(BranchCopyUnavailableException.class)
                .hasMessageContaining("worktree list");
    }
}

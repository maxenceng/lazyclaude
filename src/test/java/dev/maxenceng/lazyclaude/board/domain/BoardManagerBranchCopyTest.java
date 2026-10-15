package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.CheckedOutBranchFixture.checkedOut;
import static dev.maxenceng.lazyclaude.board.domain.CheckedOutBranchFixture.closeBranch;
import static dev.maxenceng.lazyclaude.board.domain.CheckedOutBranchFixture.trunk;
import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.doneTicketFile;
import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.inReviewTicketFile;
import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFile;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.WorktreeFixture.detachedTicketWorktree;
import static dev.maxenceng.lazyclaude.board.domain.WorktreeFixture.rootCheckoutOn;
import static dev.maxenceng.lazyclaude.board.domain.WorktreeFixture.ticketWorktreeOf;
import static dev.maxenceng.lazyclaude.board.domain.WorktreeFixture.trunkTicketWorktree;
import static dev.maxenceng.lazyclaude.board.domain.WorktreeFixture.worktreeOn;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.fileName;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.prefixSharingTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;

@ExtendWith(MockitoExtension.class)
class BoardManagerBranchCopyTest {

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private BranchCopyPort copies;

    @InjectMocks
    private BoardManager manager;

    @BeforeEach
    void readTheRootCopyAsTodo() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));
    }

    Board board(Set<TicketBranch> merged) {
        return manager.boardFor(workingDirectory(), merged);
    }

    @Test
    void shouldShowTheCopyOfAWorktreeOnTheTicketBranch() {
        Worktree worktree = worktreeOn(checkedOut(ticketBranch()));
        when(copies.worktrees(project())).thenReturn(List.of(worktree));
        when(copies.inWorktree(worktree, fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        assertThat(board(Set.of()).groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(inReviewTicketFile().ticket());
        assertThat(board(Set.of()).groups().get(Status.TODO.ordinal()).tickets()).isEmpty();
    }

    @Test
    void shouldShowTheCopyOfTheRootCheckoutWhenItIsOnTheTicketBranch() {
        Worktree root = rootCheckoutOn(checkedOut(ticketBranch()));
        when(copies.worktrees(project())).thenReturn(List.of(root));
        when(copies.inWorktree(root, fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        assertThat(board(Set.of()).groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(inReviewTicketFile().ticket());
    }

    @Test
    void shouldShowTheCopyOfTheTicketWorktreeWhateverBranchItIsOn() {
        Worktree worktree = trunkTicketWorktree();
        when(copies.worktrees(project())).thenReturn(List.of(worktree));
        when(copies.inWorktree(worktree, fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        assertThat(board(Set.of()).groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(inReviewTicketFile().ticket());
    }

    @Test
    void shouldShowTheCopyOfTheTicketWorktreeWhenItIsDetached() {
        Worktree worktree = detachedTicketWorktree();
        when(copies.worktrees(project())).thenReturn(List.of(worktree));
        when(copies.inWorktree(worktree, fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        assertThat(board(Set.of()).groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(inReviewTicketFile().ticket());
    }

    @Test
    void shouldShowTheBranchCopyWhenNoWorktreeHasTheBranch() {
        when(copies.worktrees(project())).thenReturn(List.of(rootCheckoutOn(trunk())));
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        assertThat(board(Set.of()).groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(inReviewTicketFile().ticket());
    }

    @Test
    void shouldShowTheRootCopyWhenThereIsNoBranchCopy() {
        when(copies.worktrees(project())).thenReturn(List.of());
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.empty());

        assertThat(board(Set.of()).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketFile().ticket());
    }

    @Test
    void shouldPreferTheWorktreeCopyToTheBranchCopy() {
        Worktree worktree = worktreeOn(checkedOut(ticketBranch()));
        when(copies.worktrees(project())).thenReturn(List.of(worktree));
        when(copies.inWorktree(worktree, fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        board(Set.of());

        verify(copies, never()).onBranch(any(), any(), any());
    }

    @Test
    void shouldShowTheRootCopyWhenTheWorktreeLacksTheFileEvenThoughTheBranchHasIt() {
        Worktree worktree = worktreeOn(checkedOut(ticketBranch()));
        when(copies.worktrees(project())).thenReturn(List.of(worktree));
        when(copies.inWorktree(worktree, fileName())).thenReturn(Optional.empty());

        assertThat(board(Set.of()).groups().get(Status.TODO.ordinal()).tickets())
                .as("a worktree that has the branch speaks for it; a missing file there is a missing file")
                .containsExactly(ticketFile().ticket());
        verify(copies, never()).onBranch(any(), any(), any());
    }

    @Test
    void shouldNeverTakeAWorktreeOnABranchThatOnlySharesTheIdPrefix() {
        when(copies.worktrees(project())).thenReturn(List.of(worktreeOn(checkedOut(prefixSharingTicketBranch()))));
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.empty());

        assertThat(board(Set.of()).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketFile().ticket());
        verify(copies).worktrees(project());
        verify(copies).onBranch(project(), ticketBranch(), fileName());
        verifyNoMoreInteractions(copies);
    }

    @Test
    void shouldNeverTakeAWorktreeAtThePathOfATicketBranchThatOnlySharesTheIdPrefix() {
        when(copies.worktrees(project()))
                .thenReturn(List.of(ticketWorktreeOf(prefixSharingTicketBranch(), trunk())));
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.empty());

        board(Set.of());

        verify(copies).onBranch(project(), ticketBranch(), fileName());
        verifyNoMoreInteractions(copies);
    }

    @Test
    void shouldNeverTakeAWorktreeOnABranchThatIsNotATicketBranch() {
        when(copies.worktrees(project())).thenReturn(List.of(worktreeOn(closeBranch())));
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.empty());

        board(Set.of());

        verify(copies).onBranch(project(), ticketBranch(), fileName());
        verifyNoMoreInteractions(copies);
    }

    @Test
    void shouldShowTheRootCopyOfATicketWhoseBranchIsMerged() {
        when(copies.worktrees(project())).thenReturn(List.of(rootCheckoutOn(trunk())));

        assertThat(board(Set.of(ticketBranch())).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketFile().ticket());
        verify(copies, never()).onBranch(any(), any(), any());
    }

    @Test
    void shouldShowTheTicketWorktreeCopyOfATicketWhoseBranchIsMerged() {
        Worktree worktree = trunkTicketWorktree();
        when(copies.worktrees(project())).thenReturn(List.of(worktree));
        when(copies.inWorktree(worktree, fileName())).thenReturn(Optional.of(doneTicketFile()));

        assertThat(board(Set.of(ticketBranch())).groups().get(Status.DONE.ordinal()).tickets())
                .as("a close-out written in the ticket worktree shows before it merges, the merged branch notwithstanding")
                .containsExactly(doneTicketFile().ticket());
    }

    @Test
    void shouldStillReadTheBranchCopyOfATicketWhoseBranchIsNotMerged() {
        when(copies.worktrees(project())).thenReturn(List.of());
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        assertThat(board(Set.of(prefixSharingTicketBranch())).groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(inReviewTicketFile().ticket());
    }

    @Test
    void shouldShowARootCopyThatReadsDoneWhateverTheBranchCopySays() {
        when(backlog.entries(project())).thenReturn(List.of(doneTicketFile()));

        assertThat(board(Set.of()).groups().get(Status.DONE.ordinal()).tickets())
                .as("a kept local branch must never bring back an old status")
                .containsExactly(doneTicketFile().ticket());
        verify(copies, never()).inWorktree(any(), any());
        verify(copies, never()).onBranch(any(), any(), any());
    }

    @Test
    void shouldListARefusedBranchCopyAsUnreadableAndNeverFallBackToTheRootCopy() {
        UnreadableTicket refused = unreadableTicketBuilder().file(fileName()).build();
        when(copies.worktrees(project())).thenReturn(List.of());
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.of(refused));

        Board board = board(Set.of());

        assertThat(board.unreadable()).containsExactly(refused);
        assertThat(board.groups().get(Status.TODO.ordinal()).tickets()).isEmpty();
    }

    @Test
    void shouldListARefusedWorktreeCopyAsUnreadable() {
        UnreadableTicket refused = unreadableTicketBuilder().file(fileName()).build();
        Worktree worktree = worktreeOn(checkedOut(ticketBranch()));
        when(copies.worktrees(project())).thenReturn(List.of(worktree));
        when(copies.inWorktree(worktree, fileName())).thenReturn(Optional.of(refused));

        assertThat(board(Set.of()).unreadable()).containsExactly(refused);
    }

    @Test
    void shouldListAFailingGitShowAsUnreadableWithItsReason() {
        when(copies.worktrees(project())).thenReturn(List.of());
        when(copies.onBranch(project(), ticketBranch(), fileName()))
                .thenThrow(new BranchCopyUnavailableException("git show exited with 128"));

        Board board = board(Set.of());

        assertThat(board.unreadable())
                .containsExactly(UnreadableTicket.builder()
                        .file(fileName())
                        .reason(new Reason("git show exited with 128"))
                        .build());
        assertThat(board.groups().get(Status.TODO.ordinal()).tickets()).isEmpty();
    }

    @Test
    void shouldLetTheFailureToListWorktreesThrough() {
        when(copies.worktrees(project())).thenThrow(new BranchCopyUnavailableException("git is gone"));

        assertThatThrownBy(() -> board(Set.of()))
                .isExactlyInstanceOf(BranchCopyUnavailableException.class);
    }
}

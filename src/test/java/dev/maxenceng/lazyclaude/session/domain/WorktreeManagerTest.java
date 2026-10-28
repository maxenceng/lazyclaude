package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.domain.CommitIdFixture.commitId;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.nowhereTicketWorktree;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketWorktree;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeManagerFixture.noSessionsInTheWorktree;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeManagerFixture.oneSessionInTheWorktree;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.clearWorktreeContents;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.heldWorktree;
import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.dirtyPath;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.claudeSessionName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.published.NoMergedHead;

@ExtendWith(MockitoExtension.class)
class WorktreeManagerTest {

    @Mock
    private WorktreePort worktrees;

    private WorktreeManager manager;

    @BeforeEach
    void setUp() {
        manager = new WorktreeManager(worktrees);
    }

    @Test
    void shouldHoldNoWorktreeForAProjectWithNoFolderToKeepThemIn() {
        assertThat(manager.held(nowhereTicketWorktree(), headCommit(), noSessionsInTheWorktree())).isEmpty();
        verifyNoMoreInteractions(worktrees);
    }

    @Test
    void shouldHoldNoWorktreeWhereGitListsNone() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(false);

        assertThat(manager.held(ticketWorktree(), headCommit(), noSessionsInTheWorktree())).isEmpty();
        verifyNoMoreInteractions(worktrees);
    }

    @Test
    void shouldJudgeAWorktreeWithNoMergedPullRequestWithoutReadingIt() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);

        assertThat(manager.held(ticketWorktree(), new NoMergedHead(), noSessionsInTheWorktree()))
                .contains(heldWorktree(new NoMergedPullRequest()));
        verifyNoMoreInteractions(worktrees);
    }

    @Test
    void shouldFindAWorktreeClearWhenNothingStandsInIt() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of());
        when(worktrees.commitsBeyond(worktreeDirectory(), headCommit())).thenReturn(List.of());

        assertThat(manager.held(ticketWorktree(), headCommit(), noSessionsInTheWorktree()))
                .contains(heldWorktree(clearWorktreeContents()));
    }

    @Test
    void shouldFindWhatStandsInAWorktree() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of(dirtyPath()));
        when(worktrees.commitsBeyond(worktreeDirectory(), headCommit())).thenReturn(List.of(commitId()));

        assertThat(manager.held(ticketWorktree(), headCommit(), oneSessionInTheWorktree()))
                .contains(heldWorktree(WorktreeContents.builder()
                                .dirtyPaths(List.of(dirtyPath()))
                                .commitsBeyondMerge(List.of(commitId()))
                                .sessions(List.of(claudeSessionName()))
                                .build()));
    }

    @Test
    void shouldRemoveAClearWorktree() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of());
        when(worktrees.commitsBeyond(worktreeDirectory(), headCommit())).thenReturn(List.of());

        manager.remove(ticketWorktree(), headCommit(), noSessionsInTheWorktree());

        verify(worktrees).remove(project(), worktreeDirectory());
    }

    @Test
    void shouldRefuseToRemoveAWorktreeWithADirtyPathWhateverAskedFirst() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of(dirtyPath()));
        when(worktrees.commitsBeyond(worktreeDirectory(), headCommit())).thenReturn(List.of());

        assertThatThrownBy(() -> manager.remove(ticketWorktree(), headCommit(), noSessionsInTheWorktree()))
                .isInstanceOfSatisfying(
                        WorktreeNotClearException.class,
                        refused -> assertThat(refused.state())
                                .isEqualTo(WorktreeContents.builder()
                                        .dirtyPaths(List.of(dirtyPath()))
                                        .build()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldRefuseToRemoveAWorktreeWithALiveSessionInIt() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of());
        when(worktrees.commitsBeyond(worktreeDirectory(), headCommit())).thenReturn(List.of());

        assertThatThrownBy(() -> manager.remove(ticketWorktree(), headCommit(), oneSessionInTheWorktree()))
                .isInstanceOf(WorktreeNotClearException.class);
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldRefuseToRemoveAWorktreeWhenNoMergedPullRequestIsKnown() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);

        assertThatThrownBy(() -> manager.remove(ticketWorktree(), new NoMergedHead(), noSessionsInTheWorktree()))
                .isInstanceOfSatisfying(
                        WorktreeNotClearException.class,
                        refused -> assertThat(refused.state()).isEqualTo(new NoMergedPullRequest()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldRemoveNothingWhereThereIsNoWorktree() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(false);

        manager.remove(ticketWorktree(), headCommit(), noSessionsInTheWorktree());

        verify(worktrees, never()).remove(any(), any());
    }
}

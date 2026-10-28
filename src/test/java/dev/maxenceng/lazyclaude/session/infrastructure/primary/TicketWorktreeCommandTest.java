package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLogPort;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.worktreePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.ticketWorktreeCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.ticketWorktreeCommandOverOccupied;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.occupiedWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.dirtyPath;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.published.NoMergedHead;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotRemovedException;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;

@ExtendWith(MockitoExtension.class)
class TicketWorktreeCommandTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private WorktreePort worktrees;

    private TicketWorktreeCommand command;

    @BeforeEach
    void setUp() {
        command = ticketWorktreeCommand(sessions, emptyLaunchLogPort(), worktrees);
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
    }

    @Test
    void shouldRemoveAClearWorktree() {

        assertThat(command.remove(worktreePayload(headCommit()))).isEqualTo(new RemovedWorktreeView());
        verify(worktrees).remove(project(), worktreeDirectory());
    }

    @Test
    void shouldRefuseAWorktreeWithADirtyPathAndSayWhat() {
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of(dirtyPath()));

        assertThat(command.remove(worktreePayload(headCommit())))
                .isEqualTo(new RefusedRemovalView(dirtyWorktreeContentsView()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldRefuseAWorktreeWithAHostedSessionInItAndNameIt() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        assertThat(ticketWorktreeCommandOverOccupied(sessions, worktrees).remove(worktreePayload(headCommit())))
                .isEqualTo(new RefusedRemovalView(occupiedWorktreeContentsView()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldRefuseAWorktreeWhenNoMergedPullRequestIsKnown() {

        assertThat(command.remove(worktreePayload(new NoMergedHead())))
                .isEqualTo(new RefusedRemovalView(new NoMergedPullRequestView()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldFailWithGitsMessageWhenGitRefusesToRemove() {
        doThrow(new WorktreeNotRemovedException("git worktree remove: fatal: contains modified or untracked files"))
                .when(worktrees)
                .remove(project(), worktreeDirectory());

        assertThat(command.remove(worktreePayload(headCommit())))
                .isEqualTo(new FailedRemovalView(
                        new Reason("git worktree remove: fatal: contains modified or untracked files")));
    }

    @Test
    void shouldFailWithWhyWhenGitCannotSayWhatStandsInTheWorktree() {
        when(worktrees.dirtyPaths(worktreeDirectory())).thenThrow(new WorktreeUnavailableException("git status: fatal"));

        assertThat(command.remove(worktreePayload(headCommit())))
                .isEqualTo(new FailedRemovalView(new Reason("git status: fatal")));
        verify(worktrees, never()).remove(any(), any());
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedIn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.worktreePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.ticketWorktreeCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.occupiedWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.dirtyPath;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
        command = ticketWorktreeCommand(sessions, locator, worktrees);
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
    }

    @Test
    void shouldRemoveAClearWorktree() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(command.remove(worktreePayload(headCommit()))).isEqualTo(new RemovedWorktreeView());
        verify(worktrees).remove(project(), worktreeDirectory());
    }

    @Test
    void shouldRefuseAWorktreeWithADirtyPathAndSayWhat() {
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of(dirtyPath()));
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(command.remove(worktreePayload(headCommit())))
                .isEqualTo(new RefusedRemovalView(dirtyWorktreeContentsView()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldRefuseAWorktreeWithALiveSessionInItAndNameIt() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(worktreeDirectory())));
        when(locator.locate(worktreeDirectory())).thenReturn(project());

        assertThat(command.remove(worktreePayload(headCommit())))
                .isEqualTo(new RefusedRemovalView(occupiedWorktreeContentsView()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldRefuseAWorktreeWhenNoMergedPullRequestIsKnown() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(command.remove(worktreePayload(new NoMergedHead())))
                .isEqualTo(new RefusedRemovalView(new NoMergedPullRequestView()));
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldFailWithGitsMessageWhenGitRefusesToRemove() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
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
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(command.remove(worktreePayload(headCommit())))
                .isEqualTo(new FailedRemovalView(new Reason("git status: fatal")));
        verify(worktrees, never()).remove(any(), any());
    }
}

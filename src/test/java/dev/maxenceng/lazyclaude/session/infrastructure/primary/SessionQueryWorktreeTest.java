package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.CommitIdFixture.commitId;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLogPort;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ticketHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.worktreePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQueryOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQueryOverOccupied;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.aheadWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.clearHeldWorktreeView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.heldWorktreeView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.occupiedWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.published.DirtyPathFixture.dirtyPath;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.review.published.NoMergedHead;
import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class SessionQueryWorktreeTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private WorktreePort worktrees;

    @Mock
    private HostPort hosts;

    @Captor
    private ArgumentCaptor<Consumer<ExitCode>> exited;

    private SessionQuery query;

    @BeforeEach
    void setUp() {
        query = sessionQueryOver(sessions, emptyLaunchLogPort(), worktrees);
    }

    @Test
    void shouldViewAClearTicketWorktree() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);

        assertThat(query.worktreeOf(worktreePayload(headCommit()))).isEqualTo(clearHeldWorktreeView());
    }

    @Test
    void shouldViewTheDirtyPathsOfATicketWorktree() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.dirtyPaths(worktreeDirectory())).thenReturn(List.of(dirtyPath()));

        assertThat(query.worktreeOf(worktreePayload(headCommit())))
                .isEqualTo(heldWorktreeView(dirtyWorktreeContentsView(), false));
    }

    @Test
    void shouldViewTheCommitsBeyondTheMerge() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.commitsBeyond(worktreeDirectory(), headCommit())).thenReturn(List.of(commitId()));

        assertThat(query.worktreeOf(worktreePayload(headCommit())))
                .isEqualTo(heldWorktreeView(aheadWorktreeContentsView(), false));
    }

    @Test
    void shouldViewTheHostedSessionsInATicketWorktree() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        assertThat(sessionQueryOverOccupied(sessions, worktrees).worktreeOf(worktreePayload(headCommit())))
                .isEqualTo(heldWorktreeView(occupiedWorktreeContentsView(), false));
    }

    @Test
    void shouldViewNoMergedPullRequestWhenNoneIsKnownForTheBranch() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);

        assertThat(query.worktreeOf(worktreePayload(new NoMergedHead())))
                .isEqualTo(heldWorktreeView(new NoMergedPullRequestView(), false));
    }

    @Test
    void shouldViewNoWorktreeWhereGitListsNone() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(false);

        assertThat(query.worktreeOf(worktreePayload(headCommit()))).isEqualTo(new NoWorktreeView());
    }

    @Test
    void shouldViewWhyGitCannotSayWhatStandsInTheWorktree() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(worktrees.dirtyPaths(worktreeDirectory())).thenThrow(new WorktreeUnavailableException("git status: fatal"));

        assertThat(query.worktreeOf(worktreePayload(headCommit())))
                .isEqualTo(new UnreadableWorktreeView(new Reason("git status: fatal")));
    }

    @Test
    void shouldViewWhyGitCannotListTheWorktrees() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenThrow(new WorktreeUnavailableException("git is gone"));

        assertThat(query.worktreeOf(worktreePayload(headCommit())))
                .isEqualTo(new UnreadableWorktreeView(new Reason("git is gone")));
    }

    @Test
    void shouldHandTheListenerTheTicketWhoseDoneSessionEnded() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        SessionApplicationService service = sessionApplicationService(
                sessions,
                mock(PastSessionPort.class),
                emptyLaunchLogPort(),
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                mock(WarningPort.class),
                hosts,
                worktrees);
        List<LinkedTicketId> ended = new ArrayList<>();
        new SessionQuery(service).onTicketDoneEnded(ended::add);
        new HostedSessionCommand(service).startTicket(ticketHostedPayload(Verb.DONE));
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        assertThat(ended).containsExactly(linkedTicketId());
    }
}

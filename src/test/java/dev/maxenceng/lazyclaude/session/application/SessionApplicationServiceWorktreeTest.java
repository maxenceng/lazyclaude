package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.hostingATicketSessionInTheWorktree;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLogPort;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.quietPastSessions;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.recordUnreadable;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketStart;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketWorktree;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.clearWorktreeContents;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.heldWorktree;
import static dev.maxenceng.lazyclaude.session.domain.WorktreeStateFixture.occupiedWorktreeContents;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TicketDoneEndedListener;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotClearException;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class SessionApplicationServiceWorktreeTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private TranscriptWatchPort transcriptWatch;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Mock
    private HostPort hosts;

    @Mock
    private WorktreePort worktrees;

    @Mock
    private TicketDoneEndedListener doneEnded;

    @Captor
    private ArgumentCaptor<Consumer<ExitCode>> exited;

    private SessionApplicationService service;

    @BeforeEach
    void setUp() {
        service = sessionApplicationService(sessions, quietPastSessions(), emptyLaunchLogPort(), watch, transcriptWatch, warnings, hosts, worktrees);
    }

    @Test
    void shouldJudgeTheWorktreeAgainstTheHostedSessionsLiveInIt() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        hostingATicketSessionInTheWorktree(service, hosts, worktrees);

        assertThat(service.worktreeOf(project(), noTickets(), ticketWorktree(), headCommit()))
                .contains(heldWorktree(occupiedWorktreeContents()));
    }

    @Test
    void shouldJudgeTheWorktreeClearWhenNoSessionIsHostedInIt() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);

        assertThat(service.worktreeOf(project(), noTickets(), ticketWorktree(), headCommit()))
                .contains(heldWorktree(clearWorktreeContents()));
    }

    @Test
    void shouldJudgeTheWorktreeOccupiedByAHostedSessionWhoseRecordCannotBeRead() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());
        hostingATicketSessionInTheWorktree(service, hosts, worktrees);

        assertThat(service.worktreeOf(project(), noTickets(), ticketWorktree(), headCommit()))
                .hasValueSatisfying(held -> assertThat(held.state().clear()).isFalse());
    }

    @Test
    void shouldRemoveTheWorktreeWhenNoSessionIsHostedInIt() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);

        service.removeWorktree(project(), noTickets(), ticketWorktree(), headCommit());

        verify(worktrees).remove(project(), worktreeDirectory());
    }

    @Test
    void shouldNotRemoveTheWorktreeWhileASessionIsHostedInIt() {
        when(worktrees.isWorktreeAt(project(), worktreeDirectory())).thenReturn(true);
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        hostingATicketSessionInTheWorktree(service, hosts, worktrees);

        assertThatThrownBy(() -> service.removeWorktree(project(), noTickets(), ticketWorktree(), headCommit()))
                .isInstanceOf(WorktreeNotClearException.class);
        verify(worktrees, never()).remove(any(), any());
    }

    @Test
    void shouldTellTheListenerWhichTicketsDoneSessionEnded() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());
        service.onTicketDoneEnded(doneEnded);
        service.startTicket(ticketStart(Verb.DONE), paneSize());
        verify(hosts).spawn(any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        verify(doneEnded).ended(linkedTicketId());
    }
}

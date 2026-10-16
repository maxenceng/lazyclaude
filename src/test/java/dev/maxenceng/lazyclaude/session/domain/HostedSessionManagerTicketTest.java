package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.resumeLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.ticketLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.idOf;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.nowhereTicketWorktree;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketStart;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketWorktree;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectoryAt;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class HostedSessionManagerTicketTest {

    @Mock
    private HostPort hosts;

    @Mock
    private WarningPort warnings;

    @Mock
    private WorktreePort worktrees;

    @Captor
    private ArgumentCaptor<HostLaunch> launched;

    private HostedSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new HostedSessionManager(hosts, warnings, worktrees);
    }

    @Test
    void shouldRunTheVerbInTheWorktreeWhereTheBranchIsCheckedOut() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(ticketLaunch(idOf(start), worktreeDirectory(), Verb.REVIEW));
        verify(worktrees, never()).addOnBranch(any(), any(), any());
        verify(worktrees, never()).addOnNewBranch(any(), any(), any());
    }

    @Test
    void shouldRunTheVerbInTheRootCheckoutWhenTheBranchIsCheckedOutThere() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(projectRootDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue())
                .as("a branch cannot be checked out twice, so the session runs where it already is")
                .isEqualTo(ticketLaunch(idOf(start), projectRootDirectory(), Verb.REVIEW));
        verify(worktrees, never()).addOnBranch(any(), any(), any());
    }

    @Test
    void shouldAddTheWorktreeOnTheExistingBranchWhenItIsCheckedOutNowhere() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.empty());
        when(hosts.exists(worktreeDirectory())).thenReturn(false);
        when(worktrees.branchExists(project(), branchName())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = manager.startTicket(ticketStart(Verb.START), paneSize());

        verify(worktrees).addOnBranch(project(), branchName(), worktreeDirectory());
        verify(worktrees, never()).addOnNewBranch(any(), any(), any());
        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(ticketLaunch(idOf(start), worktreeDirectory(), Verb.START));
    }

    @Test
    void shouldAddTheWorktreeOnANewBranchWhenTheBranchDoesNotExist() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.empty());
        when(hosts.exists(worktreeDirectory())).thenReturn(false);
        when(worktrees.branchExists(project(), branchName())).thenReturn(false);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        manager.startTicket(ticketStart(Verb.START), paneSize());

        verify(worktrees).addOnNewBranch(project(), branchName(), worktreeDirectory());
        verify(worktrees, never()).addOnBranch(any(), any(), any());
    }

    @Test
    void shouldListTheStartedSessionUnderItsTicket() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());

        manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        assertThat(manager.open().sessions())
                .extracting(HostedSession::ticket)
                .containsExactly(Optional.of(linkedTicketId()));
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenThePathExistsButIsNotAWorktreeOfTheBranch() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.empty());
        when(hosts.exists(worktreeDirectory())).thenReturn(true);

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        assertThat(start).isEqualTo(new NotStartedHost());
        verify(hosts, never()).spawn(any(), any());
        verify(worktrees, never()).addOnBranch(any(), any(), any());
        verify(worktrees, never()).addOnNewBranch(any(), any(), any());
        assertThat(raisedTexts(warnings))
                .containsExactly(
                        "ticket worktree: /work/app.worktrees/PROJ-1-ticket exists but is not a worktree of PROJ-1-ticket; nothing started.");
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenAddingTheWorktreeFails() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.empty());
        when(hosts.exists(worktreeDirectory())).thenReturn(false);
        when(worktrees.branchExists(project(), branchName())).thenReturn(true);
        doThrow(new WorktreeUnavailableException("fatal: 'PROJ-1-ticket' is already used"))
                .when(worktrees)
                .addOnBranch(project(), branchName(), worktreeDirectory());

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        assertThat(start).isEqualTo(new NotStartedHost());
        verify(hosts, never()).spawn(any(), any());
        assertThat(raisedTexts(warnings))
                .containsExactly("ticket worktree: fatal: 'PROJ-1-ticket' is already used; nothing started.");
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenGitCannotSayWhereTheBranchIsCheckedOut() {
        when(worktrees.checkedOutAt(project(), branchName())).thenThrow(new WorktreeUnavailableException("git is gone"));

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        assertThat(start).isEqualTo(new NotStartedHost());
        assertThat(raisedTexts(warnings)).containsExactly("ticket worktree: git is gone; nothing started.");
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenTheProjectRootHasNoPlaceForWorktrees() {
        TicketStart start = TicketStart.builder()
                .worktree(nowhereTicketWorktree())
                .ticket(linkedTicketId())
                .verb(Verb.REVIEW)
                .build();
        when(worktrees.checkedOutAt(nowhereTicketWorktree().project(), branchName())).thenReturn(Optional.empty());

        assertThat(manager.startTicket(start, paneSize())).isEqualTo(new NotStartedHost());
        assertThat(raisedTexts(warnings))
                .containsExactly("ticket worktree: / has no folder beside it to keep worktrees in; nothing started.");
    }

    @Test
    void shouldRecreateAGoneTicketWorktreeBeforeResumingInIt() {
        when(hosts.exists(worktreeDirectory())).thenReturn(false, true);
        when(worktrees.branchExists(project(), branchName())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = manager.resumeInTicketWorktree(sessionId(), worktreeDirectory(), ticketWorktree(), paneSize());

        verify(worktrees).addOnBranch(project(), branchName(), worktreeDirectory());
        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(resumeLaunch(idOf(start), worktreeDirectory()));
    }

    @Test
    void shouldResumeWithoutTouchingWorktreesWhenTheTicketWorktreeIsThere() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        manager.resumeInTicketWorktree(sessionId(), worktreeDirectory(), ticketWorktree(), paneSize());

        verifyNoInteractions(worktrees);
    }

    @Test
    void shouldStillRefuseToResumeInAnotherGoneDirectory() {
        when(hosts.exists(workingDirectoryAt("/work/elsewhere"))).thenReturn(false);

        HostStart start = manager.resumeInTicketWorktree(sessionId(), workingDirectoryAt("/work/elsewhere"), ticketWorktree(), paneSize());

        assertThat(start).isEqualTo(new NotStartedHost());
        verifyNoInteractions(worktrees);
        assertThat(raisedTexts(warnings)).containsExactly("resume: /work/elsewhere is gone; nothing resumed.");
    }

    @Test
    void shouldNotResumeAndWarnWhenRecreatingTheGoneTicketWorktreeFails() {
        when(hosts.exists(worktreeDirectory())).thenReturn(false);
        when(worktrees.branchExists(project(), branchName())).thenReturn(true);
        doThrow(new WorktreeUnavailableException("fatal: already checked out"))
                .when(worktrees)
                .addOnBranch(project(), branchName(), worktreeDirectory());

        HostStart start = manager.resumeInTicketWorktree(sessionId(), worktreeDirectory(), ticketWorktree(), paneSize());

        assertThat(start).isEqualTo(new NotStartedHost());
        verify(hosts, never()).spawn(any(), any());
        assertThat(raisedTexts(warnings)).containsExactly("ticket worktree: fatal: already checked out; nothing started.");
    }
}

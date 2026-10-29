package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.resumeLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.ticketLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.idOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
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
    private LiveSessionPort records;

    @Mock
    private WarningPort warnings;

    @Mock
    private WorktreePort worktrees;

    @Mock
    private LaunchLogPort log;

    @Mock
    private TicketDoneEndedListener doneEnded;

    @Mock
    private OpenSessionsListener openChanged;

    @Captor
    private ArgumentCaptor<HostLaunch> launched;

    @Captor
    private ArgumentCaptor<Consumer<ExitCode>> exited;

    private HostedSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new HostedSessionManager(hosts, records, warnings, worktrees, log, clock());
    }

    @Test
    void shouldLaunchATicketSessionUnderTheIdItsTabCarries() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        verify(hosts).spawn(launched.capture(), any(), any());
        assertThat(launched.getValue().command().session()).isEqualTo(idOf(start));
    }

    @Test
    void shouldRunTheVerbInTheWorktreeWhereTheBranchIsCheckedOut() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        verify(hosts).spawn(launched.capture(), any(), any());
        assertThat(launched.getValue()).isEqualTo(ticketLaunch(idOf(start), worktreeDirectory(), Verb.REVIEW));
        verify(worktrees, never()).addOnBranch(any(), any(), any());
        verify(worktrees, never()).addOnNewBranch(any(), any(), any());
    }

    @Test
    void shouldRunTheVerbInTheRootCheckoutWhenTheBranchIsCheckedOutThere() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(projectRootDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        HostStart start = manager.startTicket(ticketStart(Verb.REVIEW), paneSize());

        verify(hosts).spawn(launched.capture(), any(), any());
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
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        HostStart start = manager.startTicket(ticketStart(Verb.START), paneSize());

        verify(worktrees).addOnBranch(project(), branchName(), worktreeDirectory());
        verify(worktrees, never()).addOnNewBranch(any(), any(), any());
        verify(hosts).spawn(launched.capture(), any(), any());
        assertThat(launched.getValue()).isEqualTo(ticketLaunch(idOf(start), worktreeDirectory(), Verb.START));
    }

    @Test
    void shouldAddTheWorktreeOnANewBranchWhenTheBranchDoesNotExist() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.empty());
        when(hosts.exists(worktreeDirectory())).thenReturn(false);
        when(worktrees.branchExists(project(), branchName())).thenReturn(false);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        manager.startTicket(ticketStart(Verb.START), paneSize());

        verify(worktrees).addOnNewBranch(project(), branchName(), worktreeDirectory());
        verify(worktrees, never()).addOnBranch(any(), any(), any());
    }

    @Test
    void shouldListTheStartedSessionUnderItsTicket() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

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
        verify(hosts, never()).spawn(any(), any(), any());
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
        verify(hosts, never()).spawn(any(), any(), any());
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
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        HostStart start = manager.resumeInTicketWorktree(sessionId(), worktreeDirectory(), ticketWorktree(), paneSize());

        verify(worktrees).addOnBranch(project(), branchName(), worktreeDirectory());
        verify(hosts).spawn(launched.capture(), any(), any());
        assertThat(launched.getValue()).isEqualTo(resumeLaunch(idOf(start), worktreeDirectory()));
    }

    @Test
    void shouldResumeWithoutTouchingWorktreesWhenTheTicketWorktreeIsThere() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

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
        verify(hosts, never()).spawn(any(), any(), any());
        assertThat(raisedTexts(warnings)).containsExactly("ticket worktree: fatal: already checked out; nothing started.");
    }

    @Test
    void shouldTellListenersWhichTicketsDoneSessionEnded() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onTicketDoneEnded(doneEnded);
        manager.startTicket(ticketStart(Verb.DONE), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        verify(doneEnded).ended(linkedTicketId());
    }

    @Test
    void shouldTellOfTheChangedSessionsBeforeTellingWhichTicketsDoneSessionEnded() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onTicketDoneEnded(doneEnded);
        manager.startTicket(ticketStart(Verb.DONE), paneSize());
        manager.onChange(openChanged);
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        InOrder told = inOrder(openChanged, doneEnded);
        told.verify(openChanged).changed();
        told.verify(doneEnded).ended(linkedTicketId());
    }

    @Test
    void shouldTellListenersEvenWhenTheDoneSessionExitedWithAFailure() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onTicketDoneEnded(doneEnded);
        manager.startTicket(ticketStart(Verb.DONE), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(1));

        verify(doneEnded).ended(linkedTicketId());
    }

    @Test
    void shouldNotTellListenersWhenASessionRunningAnotherVerbEnds() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onTicketDoneEnded(doneEnded);
        manager.startTicket(ticketStart(Verb.RESPOND), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        verifyNoInteractions(doneEnded);
    }

    @Test
    void shouldNotTellListenersWhenASessionNotStartedForATicketEnds() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onTicketDoneEnded(doneEnded);
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        verifyNoInteractions(doneEnded);
    }

    @Test
    void shouldTellListenersOnceWhenTheDoneSessionReportsItsExitTwice() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onTicketDoneEnded(doneEnded);
        manager.startTicket(ticketStart(Verb.DONE), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());
        exited.getValue().accept(new ExitCode(0));

        exited.getValue().accept(new ExitCode(0));

        verify(doneEnded, times(1)).ended(linkedTicketId());
    }
}

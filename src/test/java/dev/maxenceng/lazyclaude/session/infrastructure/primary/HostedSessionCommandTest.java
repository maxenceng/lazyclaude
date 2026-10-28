package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.newLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.resumeLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.ticketLaunch;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.newHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.resizePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.resumeHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.resumeTicketHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ticketHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.typingPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.hostedSessionCommand;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.typedA;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.HostLaunch;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.HostUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class HostedSessionCommandTest {

    @Mock
    private HostPort hosts;

    @Mock
    private WarningPort warnings;

    @Mock
    private WorktreePort worktrees;

    @Test
    void shouldStartAPlainClaudeInTheProjectRoot() {
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = hostedSessionCommand(hosts, warnings).startUnlinked(newHostedPayload());

        ArgumentCaptor<HostLaunch> launched = ArgumentCaptor.forClass(HostLaunch.class);
        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(newLaunch(((StartedHost) start).id()));
    }

    @Test
    void shouldResumeInTheDirectoryTheTranscriptRecorded() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = hostedSessionCommand(hosts, warnings).resume(resumeHostedPayload());

        ArgumentCaptor<HostLaunch> launched = ArgumentCaptor.forClass(HostLaunch.class);
        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(resumeLaunch(((StartedHost) start).id(), worktreeDirectory()));
    }

    @Test
    void shouldStartTheVerbInTheWorktreeOfTheTicketBranch() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = hostedSessionCommand(hosts, warnings, worktrees).startTicket(ticketHostedPayload(Verb.REVIEW));

        ArgumentCaptor<HostLaunch> launched = ArgumentCaptor.forClass(HostLaunch.class);
        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(ticketLaunch(((StartedHost) start).id(), worktreeDirectory(), Verb.REVIEW));
    }

    @Test
    void shouldResumeInTheTicketWorktreeAddedBackWhenGone() {
        when(hosts.exists(worktreeDirectory())).thenReturn(false, true);
        when(worktrees.branchExists(project(), branchName())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = hostedSessionCommand(hosts, warnings, worktrees).resumeInTicketWorktree(resumeTicketHostedPayload());

        verify(worktrees).addOnBranch(project(), branchName(), worktreeDirectory());
        ArgumentCaptor<HostLaunch> launched = ArgumentCaptor.forClass(HostLaunch.class);
        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(resumeLaunch(((StartedHost) start).id(), worktreeDirectory()));
    }

    @Test
    void shouldSayNoneStartedWhenTheHostCannotStartOne() {
        when(hosts.spawn(any(), any())).thenThrow(new HostUnavailableException("no pty"));

        assertThat(hostedSessionCommand(hosts, warnings).startUnlinked(newHostedPayload())).isEqualTo(new NotStartedHost());
    }

    @Test
    void shouldFeedTheSessionAKey() {
        SessionId id = sessionId();

        hostedSessionCommand(hosts, warnings).send(typingPayload(id));

        verify(hosts).send(id, typedA());
    }

    @Test
    void shouldResizeTheSessionsPty() {
        SessionId id = sessionId();

        hostedSessionCommand(hosts, warnings).resize(resizePayload(id));

        verify(hosts).resize(id, resizedPaneSize());
    }

    @Test
    void shouldEndEveryHostedSession() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        HostedSessionCommand command = hostedSessionCommand(hosts, warnings);
        SessionId id = ((StartedHost) command.startUnlinked(newHostedPayload())).id();

        command.endAll();

        verify(hosts).end(id);
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.newLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.resumeLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.idOf;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.typedA;
import static dev.maxenceng.lazyclaude.session.published.TerminalScreenFixture.screenSaying;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class HostedSessionManagerTest {

    @Mock
    private HostPort hosts;

    @Mock
    private WarningPort warnings;

    @Mock
    private WorktreePort worktrees;

    @Mock
    private OpenSessionsListener changed;

    @Mock
    private Runnable screenChanged;

    @Captor
    private ArgumentCaptor<HostLaunch> launched;

    @Captor
    private ArgumentCaptor<Consumer<ExitCode>> exited;

    private HostedSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new HostedSessionManager(hosts, warnings, worktrees);
    }

    @Test
    void shouldStartAPlainClaudeInTheProjectRootAtThePaneSize() {
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = manager.startUnlinked(project(), paneSize());

        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(newLaunch(idOf(start)));
    }

    @Test
    void shouldListTheStartedSessionByItsPidInStartOrder() {
        when(hosts.spawn(any(), any())).thenReturn(processId(), secondProcessId());

        HostedSessionId first = idOf(manager.startUnlinked(project(), paneSize()));
        HostedSessionId second = idOf(manager.startUnlinked(project(), paneSize()));

        assertThat(manager.open().sessions())
                .extracting(HostedSession::id, HostedSession::pid)
                .containsExactly(
                        tuple(first, processId()),
                        tuple(second, secondProcessId()));
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenSpawningFails() {
        when(hosts.spawn(any(), any())).thenThrow(new HostUnavailableException("claude is not on the PATH"));

        HostStart start = manager.startUnlinked(project(), paneSize());

        assertThat(start).isEqualTo(new NotStartedHost());
        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).containsExactly("hosted session: claude is not on the PATH.");
    }

    @Test
    void shouldResumeInTheRecordedDirectory() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = manager.resume(sessionId(), worktreeDirectory(), paneSize());

        verify(hosts).spawn(launched.capture(), any());
        assertThat(launched.getValue()).isEqualTo(resumeLaunch(idOf(start), worktreeDirectory()));
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldAnswerWithTheOpenSessionInsteadOfResumingTheSameSessionTwice() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());
        HostStart first = manager.resume(sessionId(), worktreeDirectory(), paneSize());

        HostStart second = manager.resume(sessionId(), worktreeDirectory(), paneSize());

        assertThat(second).as("two claude resuming one session would both write its transcript").isEqualTo(first);
        verify(hosts).spawn(any(), any());
    }

    @Test
    void shouldRefuseToResumeAndWarnWhenTheRecordedDirectoryIsGone() {
        when(hosts.exists(worktreeDirectory())).thenReturn(false);

        HostStart start = manager.resume(sessionId(), worktreeDirectory(), paneSize());

        assertThat(start)
                .as("claude --resume finds a transcript under its directory's project folder, so anywhere else it exits")
                .isEqualTo(new NotStartedHost());
        verify(hosts, never()).spawn(any(), any());
        assertThat(raisedTexts(warnings))
                .containsExactly("resume: /work/app.worktrees/PROJ-1-ticket is gone; nothing resumed.");
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenResumingFails() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenThrow(new HostUnavailableException("no pty"));

        assertThat(manager.resume(sessionId(), worktreeDirectory(), paneSize()))
                .isEqualTo(new NotStartedHost());
        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).containsExactly("hosted session: no pty.");
    }

    @Test
    void shouldDropASessionWhoseClaudeExitedZeroWithoutAWarning() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        manager.onChange(changed);
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldDropASessionWhoseClaudeExitedNonZeroAndWarnOfTheCode() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), exited.capture());

        exited.getValue().accept(new ExitCode(2));

        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).containsExactly("hosted session: claude exited with code 2.");
    }

    @Test
    void shouldKeepTheOthersInOrderWhenTheFirstExits() {
        when(hosts.spawn(any(), any())).thenReturn(processId(), secondProcessId());
        manager.startUnlinked(project(), paneSize());
        HostedSessionId second = idOf(manager.startUnlinked(project(), paneSize()));
        verify(hosts, times(2)).spawn(any(), exited.capture());

        exited.getAllValues().getFirst().accept(new ExitCode(0));

        assertThat(manager.open().sessions()).extracting(HostedSession::id).containsExactly(second);
    }

    @Test
    void shouldTellListenersWhenASessionOpensAndWhenItCloses() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        manager.onChange(changed);

        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), exited.capture());
        verify(changed, times(1)).changed();
        exited.getValue().accept(new ExitCode(0));

        verify(changed, times(2)).changed();
    }

    @Test
    void shouldNotTellListenersWhenNothingStarted() {
        when(hosts.spawn(any(), any())).thenThrow(new HostUnavailableException("no pty"));
        manager.onChange(changed);

        manager.startUnlinked(project(), paneSize());

        verify(changed, never()).changed();
    }

    @Test
    void shouldIgnoreTheExitOfASessionItNoLongerHolds() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), exited.capture());
        exited.getValue().accept(new ExitCode(0));

        exited.getValue().accept(new ExitCode(1));

        assertThat(raisedTexts(warnings)).as("a second exit report is the pty closing, not news").isEmpty();
    }

    @Test
    void shouldNotHoldASessionWhoseClaudeExitedBeforeSpawningReturned() {
        when(hosts.spawn(any(), any())).thenAnswer(spawn -> {
            Consumer<ExitCode> onExit = spawn.getArgument(1);
            onExit.accept(new ExitCode(127));
            return processId();
        });

        HostStart start = manager.startUnlinked(project(), paneSize());

        assertThat(start).as("the shell would focus a tab that never opens").isEqualTo(new NotStartedHost());
        assertThat(manager.open().sessions())
                .as("a claude that dies at once never shows up as a tab that nothing will close")
                .isEmpty();
        assertThat(raisedTexts(warnings)).containsExactly("hosted session: claude exited with code 127.");
    }

    @Test
    void shouldHandInputToTheSessionItIsFor() {
        HostedSessionId id = HostedSessionId.random();

        manager.send(id, typedA());

        verify(hosts).send(id, typedA());
    }

    @Test
    void shouldResizeTheSessionItIsFor() {
        HostedSessionId id = HostedSessionId.random();

        manager.resize(id, resizedPaneSize());

        verify(hosts).resize(id, resizedPaneSize());
    }

    @Test
    void shouldAnswerWithTheScreenTheHostHolds() {
        HostedSessionId id = HostedSessionId.random();
        when(hosts.screenOf(id)).thenReturn(screenSaying("hello"));

        assertThat(manager.screenOf(id)).isEqualTo(screenSaying("hello"));
    }

    @Test
    void shouldEndEveryOpenSessionOnQuit() {
        when(hosts.spawn(any(), any())).thenReturn(processId(), secondProcessId());
        HostedSessionId first = idOf(manager.startUnlinked(project(), paneSize()));
        HostedSessionId second = idOf(manager.startUnlinked(project(), paneSize()));

        manager.endAll();

        verify(hosts).end(first);
        verify(hosts).end(second);
    }

    @Test
    void shouldEndNothingWhenNoSessionIsOpen() {
        manager.endAll();

        verify(hosts, never()).end(any());
        verifyNoInteractions(warnings);
    }

    @Test
    void shouldPassScreenChangesOnToItsListener() {
        ArgumentCaptor<Consumer<HostedSessionId>> hostListener = ArgumentCaptor.captor();
        manager.onScreenChange(screenChanged);
        verify(hosts).onScreenChange(hostListener.capture());

        hostListener.getValue().accept(HostedSessionId.random());

        verify(screenChanged).run();
    }

    @Test
    void shouldHoldNoPidsBeforeAnyStart() {
        assertThat(manager.open().pids()).isEqualTo(List.of());
    }
}

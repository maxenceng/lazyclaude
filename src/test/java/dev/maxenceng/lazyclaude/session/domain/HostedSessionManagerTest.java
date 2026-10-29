package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.newLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostLaunchFixture.resumeLaunch;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.idOf;
import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.ptyBytesOf;
import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.typedBytes;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.earlierStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class HostedSessionManagerTest {

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
    private OpenSessionsListener changed;

    @Mock
    private PtyListener pty;

    @Captor
    private ArgumentCaptor<HostLaunch> launched;

    @Captor
    private ArgumentCaptor<Consumer<PtyBytes>> output;

    @Captor
    private ArgumentCaptor<Consumer<ExitCode>> exited;

    private HostedSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new HostedSessionManager(hosts, records, warnings, worktrees, log, clock());
    }

    @Test
    void shouldStartAPlainClaudeInTheProjectRootAtThePaneSize() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        HostStart start = manager.startUnlinked(project(), paneSize());

        verify(hosts).spawn(launched.capture(), any(), any());
        assertThat(launched.getValue()).isEqualTo(newLaunch(idOf(start)));
    }

    @Test
    void shouldListTheStartedSessionByItsPidInStartOrder() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId(), secondProcessId());

        SessionId first = idOf(manager.startUnlinked(project(), paneSize()));
        SessionId second = idOf(manager.startUnlinked(project(), paneSize()));

        assertThat(manager.open().sessions())
                .extracting(HostedSession::id, HostedSession::pid)
                .containsExactly(
                        tuple(first, processId()),
                        tuple(second, secondProcessId()));
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenSpawningFails() {
        when(hosts.spawn(any(), any(), any())).thenThrow(new HostUnavailableException("claude is not on the PATH"));

        HostStart start = manager.startUnlinked(project(), paneSize());

        assertThat(start).isEqualTo(new NotStartedHost());
        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).containsExactly("hosted session: claude is not on the PATH.");
    }

    @Test
    void shouldResumeInTheRecordedDirectory() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        HostStart start = manager.resume(project(), sessionId(), worktreeDirectory(), paneSize());

        verify(hosts).spawn(launched.capture(), any(), any());
        assertThat(launched.getValue()).isEqualTo(resumeLaunch(idOf(start), worktreeDirectory()));
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldKeepTheIdOfTheSessionItResumes() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        assertThat(idOf(manager.resume(project(), sessionId(), worktreeDirectory(), paneSize()))).isEqualTo(sessionId());
    }

    @Test
    void shouldNameEachNewSessionByAnIdOfItsOwn() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId(), secondProcessId());

        SessionId first = idOf(manager.startUnlinked(project(), paneSize()));
        SessionId second = idOf(manager.startUnlinked(project(), paneSize()));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldAnswerWithTheOpenSessionInsteadOfResumingASessionItStartedItself() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        HostStart begun = manager.startUnlinked(project(), paneSize());

        HostStart resumed = manager.resume(project(), idOf(begun), projectRootDirectory(), paneSize());

        assertThat(resumed)
                .as("a claude begun under an id is already running that session, and a second would share its transcript")
                .isEqualTo(begun);
        verify(hosts).spawn(any(), any(), any());
    }

    @Test
    void shouldAnswerWithTheOpenSessionInsteadOfResumingTheSameSessionTwice() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        HostStart first = manager.resume(project(), sessionId(), worktreeDirectory(), paneSize());

        HostStart second = manager.resume(project(), sessionId(), worktreeDirectory(), paneSize());

        assertThat(second).as("two claude resuming one session would both write its transcript").isEqualTo(first);
        verify(hosts).spawn(any(), any(), any());
    }

    @Test
    void shouldAnswerWithTheOpenSessionInsteadOfResumingTheSessionItRunsSinceAClear() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        HostStart first = manager.startUnlinked(project(), paneSize());
        when(records.recordOf(processId()))
                .thenReturn(Optional.of(reportedSessionBuilder().session(secondSessionId()).startedAt(new StartedAt(now())).build()));

        HostStart second = manager.resume(project(), secondSessionId(), worktreeDirectory(), paneSize());

        assertThat(second).as("after /clear the tab writes the record's session, so resuming it would put a second claude on it").isEqualTo(first);
        verify(hosts).spawn(any(), any(), any());
    }

    @Test
    void shouldResumeASessionThatOnlyARecordLeftByAnEarlierProcessOfTheSamePidNames() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        when(records.recordOf(processId()))
                .thenReturn(Optional.of(reportedSessionBuilder().session(secondSessionId()).startedAt(earlierStartedAt()).build()));

        manager.resume(project(), secondSessionId(), worktreeDirectory(), paneSize());

        verify(hosts, times(2)).spawn(any(), any(), any());
    }

    @Test
    void shouldKeepTheInstantItSpawnedASessionAtToTheMillisecond() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        HostedSessionManager spawning = new HostedSessionManager(
                hosts, records, warnings, worktrees, log, Clock.fixed(now().plus(Duration.ofNanos(1)), ZoneOffset.UTC));

        spawning.startUnlinked(project(), paneSize());

        assertThat(spawning.open().sessions())
                .as("the registry records a start to the millisecond, so a finer spawn instant would make a session's own record look older")
                .singleElement()
                .extracting(HostedSession::spawnedAt)
                .isEqualTo(new StartedAt(now()));
    }

    @Test
    void shouldResumeNothingWhileTheSameSessionIsStillBeingResumed() throws Exception {
        CountDownLatch spawnBegun = new CountDownLatch(1);
        CountDownLatch spawnMayReturn = new CountDownLatch(1);
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenAnswer(invocation -> {
            spawnBegun.countDown();
            spawnMayReturn.await();
            return processId();
        }).thenReturn(secondProcessId());
        CompletableFuture<HostStart> first =
                CompletableFuture.supplyAsync(() -> manager.resume(project(), sessionId(), worktreeDirectory(), paneSize()));
        spawnBegun.await();

        HostStart second = manager.resume(project(), sessionId(), worktreeDirectory(), paneSize());
        spawnMayReturn.countDown();

        assertThat(second)
                .as("the shell resumes on background threads, and two claude resuming one session would both write its transcript")
                .isInstanceOf(NotStartedHost.class);
        assertThat(first.get()).isInstanceOf(StartedHost.class);
        verify(hosts).spawn(any(), any(), any());
    }

    @Test
    void shouldRefuseToResumeAndWarnWhenTheRecordedDirectoryIsGone() {
        when(hosts.exists(worktreeDirectory())).thenReturn(false);

        HostStart start = manager.resume(project(), sessionId(), worktreeDirectory(), paneSize());

        assertThat(start)
                .as("claude --resume finds a transcript under its directory's project folder, so anywhere else it exits")
                .isEqualTo(new NotStartedHost());
        verify(hosts, never()).spawn(any(), any(), any());
        assertThat(raisedTexts(warnings))
                .containsExactly("resume: /work/app.worktrees/PROJ-1-ticket is gone; nothing resumed.");
    }

    @Test
    void shouldOpenNoSessionAndWarnWhenResumingFails() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenThrow(new HostUnavailableException("no pty"));

        assertThat(manager.resume(project(), sessionId(), worktreeDirectory(), paneSize()))
                .isEqualTo(new NotStartedHost());
        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).containsExactly("hosted session: no pty.");
    }

    @Test
    void shouldDropASessionWhoseClaudeExitedZeroWithoutAWarning() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onChange(changed);
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldDropASessionWhoseClaudeExitedNonZeroAndWarnOfTheCode() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(2));

        assertThat(manager.open().sessions()).isEmpty();
        assertThat(raisedTexts(warnings)).containsExactly("hosted session: claude exited with code 2.");
    }

    @Test
    void shouldKeepTheOthersInOrderWhenTheFirstExits() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId(), secondProcessId());
        manager.startUnlinked(project(), paneSize());
        SessionId second = idOf(manager.startUnlinked(project(), paneSize()));
        verify(hosts, times(2)).spawn(any(), any(), exited.capture());

        exited.getAllValues().getFirst().accept(new ExitCode(0));

        assertThat(manager.open().sessions()).extracting(HostedSession::id).containsExactly(second);
    }

    @Test
    void shouldTellListenersWhenASessionOpensAndWhenItCloses() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onChange(changed);

        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());
        verify(changed, times(1)).changed();
        exited.getValue().accept(new ExitCode(0));

        verify(changed, times(2)).changed();
    }

    @Test
    void shouldNotTellListenersWhenNothingStarted() {
        when(hosts.spawn(any(), any(), any())).thenThrow(new HostUnavailableException("no pty"));
        manager.onChange(changed);

        manager.startUnlinked(project(), paneSize());

        verify(changed, never()).changed();
    }

    @Test
    void shouldIgnoreTheExitOfASessionItNoLongerHolds() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());
        exited.getValue().accept(new ExitCode(0));

        exited.getValue().accept(new ExitCode(1));

        assertThat(raisedTexts(warnings)).as("a second exit report is the pty closing, not news").isEmpty();
    }

    @Test
    void shouldNotHoldASessionWhoseClaudeExitedBeforeSpawningReturned() {
        when(hosts.spawn(any(), any(), any())).thenAnswer(spawn -> {
            Consumer<ExitCode> onExit = spawn.getArgument(2);
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
    void shouldWriteBytesToTheSessionItIsFor() {
        SessionId id = SessionId.random();

        manager.write(id, typedBytes());

        verify(hosts).write(id, typedBytes());
    }

    @Test
    void shouldResizeTheSessionItIsFor() {
        SessionId id = SessionId.random();

        manager.resize(id, resizedPaneSize());

        verify(hosts).resize(id, resizedPaneSize());
    }

    @Test
    void shouldHandWhatTheSessionWroteToItsListenerAsPtyBytes() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onPty(pty);
        SessionId id = idOf(manager.startUnlinked(project(), paneSize()));
        verify(hosts).spawn(any(), output.capture(), any());

        output.getValue().accept(ptyBytesOf("hello"));

        verify(pty).output(id, ptyBytesOf("hello"));
    }

    @Test
    void shouldTellItsListenerOfALaunchBeforeTheProcessIsSpawned() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onPty(pty);

        SessionId id = idOf(manager.startUnlinked(project(), paneSize()));

        InOrder order = inOrder(pty, hosts);
        order.verify(pty).launching(id, paneSize());
        order.verify(hosts).spawn(any(), any(), any());
    }

    @Test
    void shouldTellItsListenerASessionEndedWhenItsClaudeExits() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.onPty(pty);
        SessionId id = idOf(manager.startUnlinked(project(), paneSize()));
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        verify(pty).ended(id);
    }

    @Test
    void shouldTellItsListenerASessionEndedWhenSpawningFails() {
        when(hosts.spawn(any(), any(), any())).thenThrow(new HostUnavailableException("no pty"));
        manager.onPty(pty);

        manager.startUnlinked(project(), paneSize());

        verify(pty).ended(any());
    }

    @Test
    void shouldKeepASessionClaimedUntilItsListenerWasToldItsSpawnFailed() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenThrow(new HostUnavailableException("no pty"));
        manager.onPty(pty);
        doAnswer(invocation -> manager.resume(project(), sessionId(), worktreeDirectory(), paneSize()))
                .when(pty)
                .ended(sessionId());

        manager.resume(project(), sessionId(), worktreeDirectory(), paneSize());

        verify(hosts, times(1)).spawn(any(), any(), any());
    }

    @Test
    void shouldTellItsListenerASessionEndedWhenItsClaudeExitedBeforeSpawningReturned() {
        manager.onPty(pty);
        when(hosts.spawn(any(), any(), any())).thenAnswer(invocation -> {
            Consumer<ExitCode> onExit = invocation.getArgument(2);
            onExit.accept(new ExitCode(1));
            return processId();
        });

        manager.startUnlinked(project(), paneSize());

        verify(pty).ended(any());
    }

    @Test
    void shouldEndEveryOpenSessionOnQuit() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId(), secondProcessId());
        SessionId first = idOf(manager.startUnlinked(project(), paneSize()));
        SessionId second = idOf(manager.startUnlinked(project(), paneSize()));

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
    void shouldHoldNoPidsBeforeAnyStart() {
        assertThat(manager.open().pids()).isEqualTo(List.of());
    }
}

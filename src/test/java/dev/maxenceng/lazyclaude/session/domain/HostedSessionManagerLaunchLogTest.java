package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.idOf;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.afterChange;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLog;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.endedAt;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.endedLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchEntry;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchEntryBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchLogOf;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.logUnavailable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketStart;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class HostedSessionManagerLaunchLogTest {

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

    @Captor
    private ArgumentCaptor<UnaryOperator<LaunchLog>> change;

    @Captor
    private ArgumentCaptor<Consumer<ExitCode>> exited;

    private HostedSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new HostedSessionManager(hosts, records, warnings, worktrees, log, clock());
    }

    @Test
    void shouldLogAPlainLaunchWithItsDirectoryCommandAndStart() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        SessionId started = idOf(manager.startUnlinked(project(), paneSize()));

        verify(log).change(eq(project()), any(), change.capture());
        assertThat(afterChange(change, 0, emptyLaunchLog()))
                .usingRecursiveComparison()
                .isEqualTo(launchLogOf(launchEntryBuilder()
                        .command(new NewClaude(started))
                        .startedAt(new StartedAt(now()))
                        .build()));
    }

    @Test
    void shouldLogATicketLaunchWithTheTicketAndVerbItRuns() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        when(worktrees.checkedOutAt(any(), any())).thenReturn(Optional.of(worktreeDirectory()));

        SessionId started = idOf(manager.startTicket(ticketStart(Verb.DESIGN), paneSize()));

        verify(log).change(eq(project()), any(), change.capture());
        assertThat(afterChange(change, 0, emptyLaunchLog()).entries())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.command()).isEqualTo(HostedSessionFixture.ticketClaude(started, Verb.DESIGN));
                    assertThat(entry.directory()).isEqualTo(worktreeDirectory());
                });
    }

    @Test
    void shouldLogAResumeWithoutAddingASecondEntryForAnIdAlreadyLogged() {
        when(hosts.exists(projectRootDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        manager.resume(project(), sessionId(), projectRootDirectory(), paneSize());

        verify(log).change(eq(project()), any(), change.capture());
        assertThat(afterChange(change, 0, launchLogOf(endedLaunchEntry())).entries())
                .as("the resumed id keeps its one entry, its end cleared")
                .containsExactly(launchEntry());
    }

    @Test
    void shouldLogAResumeOfAnIdNotLoggedAsAnEntryOfItsOwn() {
        when(hosts.exists(projectRootDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());

        manager.resume(project(), sessionId(), projectRootDirectory(), paneSize());

        verify(log).change(eq(project()), any(), change.capture());
        assertThat(afterChange(change, 0, emptyLaunchLog()).entries())
                .singleElement()
                .satisfies(entry -> assertThat(entry.command()).isEqualTo(new ResumeClaude(sessionId())));
    }

    @Test
    void shouldRecordTheEndOfAClaudeThatExitsWhileItsLaunchIsBeingLogged() throws InterruptedException {
        when(hosts.spawn(any(), any(), exited.capture())).thenReturn(processId());
        List<UnaryOperator<LaunchLog>> applied = new CopyOnWriteArrayList<>();
        AtomicReference<Thread> exiting = new AtomicReference<>();
        doAnswer(invocation -> {
                    exiting.set(Thread.ofPlatform().start(() -> exited.getValue().accept(new ExitCode(0))));
                    exiting.get().join(Duration.ofMillis(200));
                    return applied.add(invocation.getArgument(2));
                })
                .doAnswer(invocation -> applied.add(invocation.getArgument(2)))
                .when(log)
                .change(any(), any(), any());

        manager.startUnlinked(project(), paneSize());
        exiting.get().join();

        assertThat(applied.stream().reduce(emptyLaunchLog(), (logged, change) -> change.apply(logged), (first, second) -> second)
                        .entries())
                .as("an exit that races the launch's own write records its end after it, never before")
                .singleElement()
                .satisfies(entry -> assertThat(entry.hasNoEnd()).isFalse());
    }

    @Test
    void shouldLogNothingWhenNothingStarted() {
        when(hosts.spawn(any(), any(), any())).thenThrow(new HostUnavailableException("claude is not on the PATH"));

        manager.startUnlinked(project(), paneSize());

        verify(log, never()).change(any(), any(), any());
    }

    @Test
    void shouldLogNothingForAClaudeThatExitedBeforeSpawningReturned() {
        when(hosts.spawn(any(), any(), any())).thenAnswer(call -> {
            Consumer<ExitCode> onExit = call.getArgument(2);
            onExit.accept(new ExitCode(1));
            return processId();
        });

        manager.startUnlinked(project(), paneSize());

        verify(log, never()).change(any(), any(), any());
    }

    @Test
    void shouldRecordTheEndOfASessionWhoseClaudeExitedByItself() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        SessionId session = idOf(manager.startUnlinked(project(), paneSize()));
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(0));

        verify(log, times(2)).change(eq(project()), any(), change.capture());
        assertThat(afterChange(change, 1, launchLogOf(launchEntryBuilder().command(new NewClaude(session)).build())).entries())
                .singleElement()
                .satisfies(entry -> assertThat(entry.end()).isEqualTo(endedAt()));
    }

    @Test
    void shouldRecordTheEndOfASessionWhoseClaudeFailed() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        exited.getValue().accept(new ExitCode(2));

        verify(log, times(2)).change(eq(project()), any(), change.capture());
    }

    @Test
    void shouldRecordNoEndForASessionLazyclaudeEndedOnQuit() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());

        manager.endAll();
        exited.getValue().accept(new ExitCode(143));

        verify(log, times(1)).change(any(), any(), any());
    }

    @Test
    void shouldRecordNoEndForAnExitOfASessionItNoLongerHolds() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        verify(hosts).spawn(any(), any(), exited.capture());
        exited.getValue().accept(new ExitCode(0));

        exited.getValue().accept(new ExitCode(0));

        verify(log, times(2)).change(any(), any(), any());
    }

    @Test
    void shouldStartTheSessionWhateverTheLogDoes() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId(), secondProcessId());

        HostStart first = manager.startUnlinked(project(), paneSize());
        HostStart second = manager.startUnlinked(project(), paneSize());

        assertThat(first).isInstanceOf(StartedHost.class);
        assertThat(second).isInstanceOf(StartedHost.class);
        assertThat(manager.open().sessions()).hasSize(2);
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldRaiseAWarningNamingWhyWhenTheLogCannotBeWritten() {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        manager.startUnlinked(project(), paneSize());
        ArgumentCaptor<LaunchLogFallback> fallback = ArgumentCaptor.captor();
        verify(log).change(eq(project()), fallback.capture(), any());

        fallback.getValue().unavailable(logUnavailable());

        assertThat(raisedTexts(warnings))
                .containsExactly("launch log: cannot write /state/lazyclaude/launch.log: Permission denied.");
    }
}

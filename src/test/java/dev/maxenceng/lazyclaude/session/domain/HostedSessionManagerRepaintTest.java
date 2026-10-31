package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.repaintGap;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.resizingMeanwhile;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.startedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSizeOf;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class HostedSessionManagerRepaintTest {

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
    private TimerPort timer;

    @Captor
    private ArgumentCaptor<Runnable> restore;

    @Captor
    private ArgumentCaptor<Consumer<ExitCode>> exited;

    private HostedSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new HostedSessionManager(hosts, records, warnings, worktrees, log, timer, clock());
    }

    @Test
    void shouldShrinkThePtyByOneRowAndRestoreItAfterTheGap() {
        SessionId started = startedSession(manager, hosts);

        manager.repaint(started);

        InOrder order = inOrder(hosts, timer);
        order.verify(hosts).resize(started, paneSizeOf(100, 29));
        order.verify(timer).after(eq(repaintGap()), restore.capture());
        verify(hosts, never()).resize(started, paneSize());
        restore.getValue().run();
        verify(hosts).resize(started, paneSize());
    }

    @Test
    void shouldRestoreTheSizeTheFrontendResizedToDuringTheGap() {
        SessionId started = startedSession(manager, hosts);
        manager.repaint(started);
        verify(timer).after(eq(repaintGap()), restore.capture());

        manager.resize(started, resizedPaneSize());
        restore.getValue().run();

        verify(hosts, never()).resize(started, paneSize());
        verify(hosts, times(2)).resize(started, resizedPaneSize());
    }

    @Test
    void shouldRepaintNothingForASessionThatIsNotOpen() {
        manager.repaint(sessionId());

        verifyNoInteractions(timer);
        verify(hosts, never()).resize(any(), any());
    }

    @Test
    void shouldNotRestoreASessionThatEndedDuringTheGap() {
        SessionId started = startedSession(manager, hosts);
        verify(hosts).spawn(any(), any(), exited.capture());
        manager.repaint(started);
        verify(timer).after(eq(repaintGap()), restore.capture());

        exited.getValue().accept(new ExitCode(0));
        restore.getValue().run();

        verify(hosts, never()).resize(started, paneSize());
    }

    @Test
    void shouldHoldAResizeUntilTheRestoreHasAppliedItsSize() throws InterruptedException {
        SessionId started = startedSession(manager, hosts);
        manager.repaint(started);
        verify(timer).after(eq(repaintGap()), restore.capture());
        AtomicReference<Thread> resizing = new AtomicReference<>();
        AtomicBoolean overtook = new AtomicBoolean();
        doAnswer(resizingMeanwhile(manager, started, resizing, overtook)).when(hosts).resize(started, paneSize());

        restore.getValue().run();
        resizing.get().join();

        assertThat(overtook)
                .as("a resize that lands while the restore applies the size it read is undone by that older size")
                .isFalse();
        InOrder order = inOrder(hosts);
        order.verify(hosts).resize(started, paneSize());
        order.verify(hosts).resize(started, resizedPaneSize());
    }
}

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.startingTabNumbered;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellInDock;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellInDockRunningOn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWatchedSessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionPayload;
import dev.maxenceng.lazyclaude.session.published.Busy;

@ExtendWith(MockitoExtension.class)
class ShellWatchedTest {

    @Mock
    private WatchedSessionCommand watching;

    @Test
    void shouldWatchTheSelectedTabsSessionWhileTheDockHasFocus() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "one", new Busy())),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                watching,
                blankScreens());

        Screen.of(shell);

        verify(watching).watching(new WatchedSessionPayload(sessionIdOf(1)));
    }

    @Test
    void shouldWatchNothingWhileAnotherPanelHasFocus() {
        Shell shell = shellHosting(
                sessionsViewWithTabs(hostedTab(1, "one", new Busy())),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                watching,
                blankScreens());

        Screen.of(shell);

        verifyNoInteractions(watching);
    }

    @Test
    void shouldStopWatchingWhenTheTerminalLosesFocusAndWatchAgainWhenItComesBack() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "one", new Busy())),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                watching,
                blankScreens());
        Screen.of(shell);

        shell.terminalFocus().lost();
        Screen.of(shell);
        shell.terminalFocus().gained();
        Screen.of(shell);

        verify(watching, times(2)).watching(new WatchedSessionPayload(sessionIdOf(1)));
        verify(watching).watching(new NoWatchedSessionPayload());
    }

    @Test
    void shouldStopWatchingWhenFocusLeavesTheDock() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "one", new Busy())),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                watching,
                blankScreens());
        Screen.of(shell);

        shell.press(alt('k'));
        Screen.of(shell);

        verify(watching).watching(new NoWatchedSessionPayload());
    }

    @Test
    void shouldTellOnlyWhenWhatIsWatchedChanges() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "one", new Busy())),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                watching,
                blankScreens());

        Screen.of(shell);
        Screen.of(shell);
        Screen.of(shell);

        verify(watching, times(1)).watching(new WatchedSessionPayload(sessionIdOf(1)));
    }

    @Test
    void shouldWatchNothingForATabWhoseSessionHasNoRegistryRecordYet() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(startingTabNumbered(1)),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                watching,
                blankScreens());

        Screen.of(shell);

        verifyNoInteractions(watching);
    }

    @Test
    void shouldLeaveTheLastChangeInForceHoweverTheBackgroundOrdersThem() {
        HeldTasksFixture background = new HeldTasksFixture();
        Shell shell = shellInDockRunningOn(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), watching, background);
        Screen.of(shell);
        shell.terminalFocus().lost();
        Screen.of(shell);

        background.runNewestFirst();

        InOrder told = inOrder(watching);
        told.verify(watching).watching(new WatchedSessionPayload(sessionIdOf(1)));
        told.verify(watching).watching(new NoWatchedSessionPayload());
    }
}

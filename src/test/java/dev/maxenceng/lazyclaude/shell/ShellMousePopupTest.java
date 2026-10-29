package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.clearHeldWorktreeView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.leftPress;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnDoneTicketWithWorktree;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellWithThePickerOpenOnAnInReviewTicket;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.startedTicketPayload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.tamboui.layout.Position;
import dev.tamboui.tui.event.MouseEvent;

class ShellMousePopupTest {

    private static final Position IN_THE_MAIN_VIEW = new Position(40, 4);

    private final HostedSessionCommand hosting = mock(HostedSessionCommand.class, ShellFixture::noHostStarted);

    @Test
    void shouldMoveThePickersCursorToTheRowClicked() {
        Shell shell = shellWithThePickerOpenOnAnInReviewTicket(hosting);

        assertThat(shell.mouse(leftPress(Screen.of(shell).positionOf("  done")))).isTrue();

        assertThat(Screen.of(shell).text()).contains("│   respond").contains("│ › done");
        verify(hosting, never()).startTicket(any());
    }

    @Test
    void shouldStartTheVerbOnASecondClickOnItsRow() {
        when(hosting.startTicket(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellWithThePickerOpenOnAnInReviewTicket(hosting);
        shell.mouse(leftPress(Screen.of(shell).positionOf("  done")));

        shell.mouse(leftPress(Screen.of(shell).positionOf("› done")));

        assertThat(startedTicketPayload(hosting).verb()).isEqualTo(Verb.DONE);
        assertThat(Screen.of(shell).text()).doesNotContain("enter start · esc cancel");
    }

    @Test
    void shouldStartTheVerbTheCursorStartsOnWithAClickOnItsRow() {
        when(hosting.startTicket(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellWithThePickerOpenOnAnInReviewTicket(hosting);

        shell.mouse(leftPress(Screen.of(shell).positionOf("› respond")));

        assertThat(startedTicketPayload(hosting).verb()).isEqualTo(Verb.RESPOND);
    }

    @Test
    void shouldDoNothingForAClickOutsideThePickerAndReachNothingUnderIt() {
        Shell shell = shellWithThePickerOpenOnAnInReviewTicket(hosting);
        Position underneath = Screen.of(shell).positionInLeftColumn("PROJ-1");

        assertThat(shell.mouse(leftPress(underneath))).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).contains("enter start · esc cancel").contains("│ › respond");
        verify(hosting, never()).startTicket(any());
    }

    @Test
    void shouldDoNothingForAClickOnThePickersTitleOrHint() {
        Shell shell = shellWithThePickerOpenOnAnInReviewTicket(hosting);
        Screen drawn = Screen.of(shell);

        shell.mouse(leftPress(drawn.positionOf("PROJ-1 · in-review")));
        shell.mouse(leftPress(drawn.positionOf("enter start")));

        assertThat(Screen.of(shell).text()).contains("│ › respond");
        verify(hosting, never()).startTicket(any());
    }

    @Test
    void shouldKeepThePickerOpenWhenTheWheelTurnsOutsideIt() {
        Shell shell = shellWithThePickerOpenOnAnInReviewTicket(hosting);
        Position underneath = Screen.of(shell).positionInLeftColumn("PROJ-1");

        shell.mouse(MouseEvent.scrollDown(underneath.x(), underneath.y()));

        assertThat(Screen.of(shell).text()).contains("enter start · esc cancel").contains("│ › respond");
    }

    @Test
    void shouldReachNothingUnderTheQuitPrompt() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "alpha", new Busy())), hosting, mock(HostedEmulatorCommand.class), blankScreens());
        shell.press(key('q'));
        Position underneath = Screen.of(shell).positionInLeftColumn("draft");
        assertThat(Screen.of(shell).statusLine()).contains("quit");

        assertThat(shell.mouse(leftPress(new Position(underneath.x(), underneath.y() + 1)))).isTrue();

        assertThat(Screen.of(shell).statusLine()).contains("quit");
        assertThat(shell.quitRequested()).isFalse();
    }

    @Test
    void shouldReachNothingUnderTheRemovalPrompt() {
        TicketWorktreeCommand removing = mock(TicketWorktreeCommand.class);
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.press(key('d'));
        assertThat(Screen.of(shell).statusLine()).contains("remove worktree");

        assertThat(shell.mouse(leftPress(IN_THE_MAIN_VIEW))).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).statusLine()).contains("remove worktree");
        verify(removing, never()).remove(any());
    }
}

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewIn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.leftPress;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.tamboui.layout.Position;

class ShellMouseSessionsTest {

    private final HostedSessionCommand hosting = mock(HostedSessionCommand.class, ShellFixture::noHostStarted);

    @Test
    void shouldOnlyFocusSessionsOnAFirstClickOnAPastSessionTheCursorAlreadyStandsOn() {
        Shell shell = shellHosting(sessionsViewOf(List.of(), List.of(pastSessionView())), hosting, blankScreens());

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("Refine 1")));

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        verify(hosting, never()).resume(any());
    }

    @Test
    void shouldResumeAPastSessionOnASecondClick() {
        Shell shell = shellHosting(sessionsViewOf(List.of(), List.of(pastSessionView())), hosting, blankScreens());
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("Refine 1")));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("Refine 1")));

        verify(hosting).resume(any());
    }

    @Test
    void shouldFocusATabOnASecondClickOnALiveSession() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "alpha", new Busy())), hosting, blankScreens());
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("alpha")));
        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("alpha")));

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldOpenTheVerbPickerOnASecondClickOnTheNewSessionRow() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("n new: /ticket")));

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("n new: /ticket")));

        assertThat(Screen.of(shell).text()).contains("╭ #1 · in-review", "enter start · esc cancel");
    }

    @Test
    void shouldNotSelectASectionHeaderWithAClick() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("n new: /ticket")));

        shell.mouse(leftPress(new Position(2, Screen.of(shell).lineOf("╭Sessions") + 1)));

        assertThat(Screen.of(shell).reversedText()).containsExactly("n new: /ticket respond");
    }
}

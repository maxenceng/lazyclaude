package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedNamedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedPastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicketSessions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class ShellEnterTicketTest {

    @Test
    void shouldMoveToTheSessionsPanelOnTheSessionWithTheStrongestMarker() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "busy-one", new Busy(), ticketLink("1", "start")),
                linkedNamedSessionView(2, "calm-one", new YourTurn(), ticketLink("1", "start")),
                linkedNamedSessionView(3, "stuck-one", waitingForPermission(), ticketLink("1", "start")),
                linkedNamedSessionView(4, "asking-one", waitingForInput(), ticketLink("1", "start")))));

        assertThat(shell.press(enter())).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("stuck-one");
    }

    @Test
    void shouldGoToTheFirstRowAmongSessionsTiedOnTheStrongestMarker() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "busy-one", new Busy(), ticketLink("1", "start")),
                linkedNamedSessionView(2, "first-asker", waitingForInput(), ticketLink("1", "start")),
                linkedNamedSessionView(3, "second-asker", waitingForInput(), ticketLink("1", "start")))));

        shell.press(enter());

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("first-asker");
    }

    @Test
    void shouldLandOnTheNewSessionRowWhenNoSessionHasAMarker() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "busy-one", new Busy(), ticketLink("1", "start")),
                        linkedNamedSessionView(2, "loose-asker", waitingForInput())),
                List.of(linkedPastSessionView(1, ticketLink("1", "start")))));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("n new: /ticket refine");
    }

    @Test
    void shouldLandOnTheNewSessionRowWhenTheTicketHasNoSession() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(linkedNamedSessionView(1, "loose-asker", waitingForInput()))));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("n new: /ticket refine");
    }

    @Test
    void shouldIgnoreASessionAskingThatIsLinkedToAnotherTicket() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "mine-one", new YourTurn(), ticketLink("1", "start")),
                linkedNamedSessionView(2, "other-one", waitingForPermission(), ticketLink("2", "start")))));

        shell.press(enter());

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("mine-one");
    }

    @Test
    void shouldStillFoldAStatusHeaderOnEnterWithoutMovingFocus() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of()));
        shell.press(key('k'));

        assertThat(shell.press(enter())).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }
}

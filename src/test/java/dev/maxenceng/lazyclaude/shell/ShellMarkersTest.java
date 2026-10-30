package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedNamedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedPastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicketSessions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.style.Color;

class ShellMarkersTest {

    @Test
    void shouldShowTheStrongestMarkerOfATicketsLiveSessionsInItsGutterAndItsColour() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "calm-one", new YourTurn(), ticketLink("1", "start")),
                linkedNamedSessionView(2, "stuck-one", waitingForPermission(), ticketLink("1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("! #1 .*"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("#1 A board"), Color.RED)).isEqualTo("!");
    }

    @Test
    void shouldShowAQuestionMarkInYellowForASessionWaitingForInput() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "asking-one", waitingForInput(), ticketLink("1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("\\? #1 .*"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("#1 A board"), Color.YELLOW)).isEqualTo("?");
    }

    @Test
    void shouldShowABulletInCyanForASessionWhoseTurnItIs() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "done-one", new YourTurn(), ticketLink("1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("• #1 .*"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("#1 A board"), Color.CYAN)).contains("•");
    }

    @Test
    void shouldShowNoMarkerForATicketWhoseSessionsAreAllBusyUnknownOrPast() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "busy-one", new Busy(), ticketLink("1", "start")),
                        linkedNamedSessionView(2, "unknown-one", new Unknown(), ticketLink("1", "start"))),
                List.of(linkedPastSessionView(1, ticketLink("1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).startsWith("  #1 "));
    }

    @Test
    void shouldShowNoMarkerForATicketWhenTheSessionWaitingIsLinkedToAnotherOne() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "stuck-one", waitingForPermission(), ticketLink("2", "start"))))));

        assertThat(screen.boardPanelLines())
                .anySatisfy(line -> assertThat(line).startsWith("  #1 "))
                .anySatisfy(line -> assertThat(line).startsWith("! #2 "));
    }

    @Test
    void shouldCountEachMarkerOfTheProjectsLiveSessionsOnTheLeftOfTheStatusLine() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "calm-one", new YourTurn(), ticketLink("1", "start")),
                linkedNamedSessionView(2, "loose-one", waitingForInput()),
                linkedNamedSessionView(3, "asking-one", waitingForInput()),
                linkedNamedSessionView(4, "stuck-one", waitingForPermission()),
                linkedNamedSessionView(5, "busy-one", new Busy())))));

        assertThat(screen.statusLine()).startsWith("! 1  ? 2  • 1");
        assertThat(screen.foregroundTextOnLine(0, Color.YELLOW)).isEqualTo("? 2");
        assertThat(screen.foregroundTextOnLine(0, Color.RED)).isEqualTo("! 1");
    }

    @Test
    void shouldListOnlyTheMarkersThatArePresent() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(linkedNamedSessionView(1, "loose-one", waitingForInput())))));

        assertThat(screen.statusLine()).startsWith("? 1").doesNotContain("!").doesNotContain("•");
    }

    @Test
    void shouldShowNothingOnTheStatusLineWhenNoLiveSessionAsksAnything() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(
                List.of(linkedNamedSessionView(1, "busy-one", new Busy())), List.of(linkedPastSessionView(1)))));

        assertThat(screen.statusLine()).isBlank();
    }

    @Test
    void shouldKeepTheWarningOnTheRightBesideTheMarker() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(linkedNamedSessionView(1, "stuck-one", waitingForPermission()))));
        shell.warningsChanged(warningView());

        Screen screen = Screen.of(shell);

        assertThat(screen.statusLine()).startsWith("! 1").endsWith(warningView().text());
    }

    @Test
    void shouldDropTheMarkerFromTheStatusLineWhenTheSessionStopsAsking() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(linkedNamedSessionView(1, "stuck-one", waitingForPermission()))));

        shell.sessionsChanged(sessionsViewOf(List.of(linkedNamedSessionView(1, "stuck-one", new Busy()))));

        assertThat(Screen.of(shell).statusLine()).isBlank();
    }
}

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedNamedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedPastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.strongLink;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.weakLink;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicketSessions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.style.Color;

class ShellMarkersTest {

    @Test
    void shouldShowTheStrongestMarkerOfATicketsLiveSessionsInItsGutterAndItsColour() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "calm-one", new YourTurn(), strongLink("PROJ-1", "start")),
                linkedNamedSessionView(2, "stuck-one", new WaitingForPermission(), strongLink("PROJ-1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("! PROJ-1 .*"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("PROJ-1 A board"), Color.RED)).isEqualTo("!");
    }

    @Test
    void shouldShowAQuestionMarkInYellowForASessionWaitingForInput() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "asking-one", waitingForInput(), strongLink("PROJ-1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("\\? PROJ-1 .*"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("PROJ-1 A board"), Color.YELLOW)).isEqualTo("?");
    }

    @Test
    void shouldShowABulletInCyanForASessionWhoseTurnItIs() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "done-one", new YourTurn(), strongLink("PROJ-1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("• PROJ-1 .*"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("PROJ-1 A board"), Color.CYAN)).contains("•");
    }

    @Test
    void shouldCountAWeakLinkForTheMarker() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "branch-one", new WaitingForPermission(), weakLink("PROJ-1"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("! PROJ-1 .*"));
    }

    @Test
    void shouldShowNoMarkerForATicketWhoseSessionsAreAllBusyUnknownOrPast() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "busy-one", new Busy(), strongLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "unknown-one", new Unknown(), strongLink("PROJ-1", "start"))),
                List.of(linkedPastSessionView(1, strongLink("PROJ-1", "start"))))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).startsWith("  PROJ-1 "));
    }

    @Test
    void shouldShowNoMarkerForATicketWhenTheSessionWaitingIsLinkedToAnotherOne() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "stuck-one", new WaitingForPermission(), strongLink("PROJ-2", "start"))))));

        assertThat(screen.boardPanelLines())
                .anySatisfy(line -> assertThat(line).startsWith("  PROJ-1 "))
                .anySatisfy(line -> assertThat(line).startsWith("! PROJ-2 "));
    }

    @Test
    void shouldShowTheStrongestMarkerOfEveryLiveSessionOnTheLeftOfTheStatusLine() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(List.of(
                linkedNamedSessionView(1, "calm-one", new YourTurn(), strongLink("PROJ-1", "start")),
                linkedNamedSessionView(2, "loose-one", waitingForInput())))));

        assertThat(screen.statusLine()).startsWith("?");
        assertThat(screen.foregroundTextOnLine(0, Color.YELLOW)).isEqualTo("?");
    }

    @Test
    void shouldShowNothingOnTheStatusLineWhenNoLiveSessionAsksAnything() {
        Screen screen = Screen.of(shellOnTicketSessions(sessionsViewOf(
                List.of(linkedNamedSessionView(1, "busy-one", new Busy())), List.of(linkedPastSessionView(1)))));

        assertThat(screen.statusLine()).isBlank();
    }

    @Test
    void shouldKeepTheWarningOnTheRightBesideTheMarker() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(linkedNamedSessionView(1, "stuck-one", new WaitingForPermission()))));
        shell.warningsChanged(warningView());

        Screen screen = Screen.of(shell);

        assertThat(screen.statusLine()).startsWith("!").endsWith(warningView().text());
    }

    @Test
    void shouldDropTheMarkerFromTheStatusLineWhenTheSessionStopsAsking() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(linkedNamedSessionView(1, "stuck-one", new WaitingForPermission()))));

        shell.sessionsChanged(sessionsViewOf(List.of(linkedNamedSessionView(1, "stuck-one", new Busy()))));

        assertThat(Screen.of(shell).statusLine()).isBlank();
    }
}

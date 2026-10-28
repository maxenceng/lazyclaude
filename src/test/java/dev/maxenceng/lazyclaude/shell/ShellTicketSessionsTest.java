package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedNamedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedPastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewsNumbered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicketSessions;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicketSessionsPanel;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class ShellTicketSessionsTest {

    @Test
    void shouldShowTheSelectedTicketsSectionUnderADimHeaderWithItsId() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(linkedNamedSessionView(1, "refiner", new Busy(), ticketLink("PROJ-1", "refine")))));

        Screen screen = Screen.of(shell);

        assertThat(screen.boardPanelText()).containsSubsequence("PROJ-1", "refiner");
        assertThat(screen.dimText()).contains("PROJ-1");
    }

    @Test
    void shouldListALiveSessionOfTheTicketBeforeItsPastOnesWhateverTheirAges() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(linkedNamedSessionView(1, "live-one", new Busy(), ticketLink("PROJ-1", "start"))),
                List.of(linkedPastSessionView(1, ticketLink("PROJ-1", "refine")))));

        assertThat(Screen.of(shell).boardPanelText()).containsSubsequence("live-one", "Past 1");
    }

    @Test
    void shouldShowEveryLinkedPastSessionWithoutAFold() {
        List<PastSessionView> past = IntStream.rangeClosed(1, 8)
                .mapToObj(number -> linkedPastSessionView(number, ticketLink("PROJ-1", "start")))
                .toList();
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(List.of(), past));
        pressed(shell, key('j'), 7);

        assertThat(Screen.of(shell).boardPanelText()).contains("Past 8").doesNotContain("older");
    }

    @Test
    void shouldFollowTheTicketsSectionWithTheUnlinkedOneUnderADimHeader() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "linked-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "loose-one", new Busy())),
                pastSessionViewsNumbered(2)));

        Screen screen = Screen.of(shell);

        assertThat(screen.boardPanelText()).containsSubsequence("PROJ-1", "linked-one", "unlinked", "loose-one", "Past session 1");
        assertThat(screen.dimText()).contains("unlinked");
    }

    @Test
    void shouldFoldTheUnlinkedPastSessionsBeyondTheNewestFiveUnderACountOfTheOlderOnes() {
        Shell shell = shellOnTicketSessions(sessionsViewOf(List.of(), pastSessionViewsNumbered(7)));
        shell.press(key('k'));
        shell.press(alt('j'));

        assertThat(Screen.of(shell).boardPanelText()).contains("Past session 5", "… 2 older").doesNotContain("Past session 6");
    }

    @Test
    void shouldListASessionLinkedToTwoTicketsUnderEachOfThem() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(linkedNamedSessionView(1, "both-ones", new Busy(), ticketLink("PROJ-1", "review"), ticketLink("PROJ-2", "review")))));

        assertThat(Screen.of(shell).boardPanelText()).contains("both-ones");
        shell.press(alt('k'));
        shell.press(key('j'));
        shell.press(alt('j'));

        assertThat(Screen.of(shell).boardPanelText()).containsSubsequence("PROJ-2", "both-ones");
    }

    @Test
    void shouldReadTheVerbBeforeTheTitleOfALinkedSession() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(linkedNamedSessionView(1, "refiner", new YourTurn(), ticketLink("PROJ-1", "refine"))),
                List.of(linkedPastSessionView(1, ticketLink("PROJ-1", "start")))));

        assertThat(Screen.of(shell).boardPanelLines())
                .anySatisfy(line -> assertThat(line).matches("• refine\\s+refiner\\s+pane"))
                .anySatisfy(line -> assertThat(line).matches("○ start\\s+Past 1\\s+10m"));
    }

    @Test
    void shouldKeepTheTextOfAnUnlinkedRowWithNoVerb() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(List.of(linkedNamedSessionView(1, "loose-one", new Busy()))));

        assertThat(Screen.of(shell).boardPanelLines()).anySatisfy(line -> assertThat(line).matches("∼ loose-one\\s+pane"));
    }

    @Test
    void shouldShowADimNoSessionRowUnderATicketWithNoLinkedSession() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(linkedNamedSessionView(1, "other-one", new Busy(), ticketLink("PROJ-2", "start")))));

        Screen screen = Screen.of(shell);

        assertThat(screen.boardPanelText()).containsSubsequence("PROJ-1", "no session");
        assertThat(screen.dimText()).contains("no session");
        assertThat(screen.reversedText()).singleElement().asString().contains("no session");
    }

    @Test
    void shouldShowOnlyTheUnlinkedSectionWithNoTicketSelected() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "linked-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "loose-one", new Busy()))));
        shell.press(alt('k'));
        shell.press(key('k'));
        shell.press(alt('j'));

        assertThat(Screen.of(shell).boardPanelText())
                .contains("unlinked", "loose-one")
                .doesNotContain("linked-one")
                .doesNotContain("no session");
    }

    @Test
    void shouldStartTheSelectionOnTheFirstRowBelowTheHeader() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "linked-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "loose-one", new Busy()))));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("linked-one");
    }

    @Test
    void shouldSkipTheHeaderRowsWithJAndK() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "linked-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "loose-one", new Busy()))));

        shell.press(key('j'));
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("n new");
        shell.press(key('j'));
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("loose-one");
        shell.press(key('j'));
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("loose-one");
        shell.press(key('k'));
        shell.press(key('k'));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("linked-one");
    }

    @Test
    void shouldFollowTheSessionIdWhenAnotherLinkedSessionAppearsAbove() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "first-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "second-one", new Busy(), ticketLink("PROJ-1", "start")))));
        shell.press(key('j'));

        shell.sessionsChanged(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(3, "newest-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(1, "first-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "second-one", new Busy(), ticketLink("PROJ-1", "start")))));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("second-one");
    }

    @Test
    void shouldMoveASessionUnderItsTicketOnceItsTranscriptNamesItAndKeepItSelected() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(List.of(linkedNamedSessionView(1, "late-one", new Busy()))));

        shell.sessionsChanged(sessionsViewOf(List.of(linkedNamedSessionView(1, "late-one", new Busy(), ticketLink("PROJ-1", "start")))));

        assertThat(Screen.of(shell).boardPanelText()).containsSubsequence("PROJ-1", "late-one").doesNotContain("unlinked");
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("late-one");
    }

    @Test
    void shouldGoToTheFirstRowOfAnotherTicketsSectionWhenTheBoardSelectionMoves() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(
                        linkedNamedSessionView(1, "one-one", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(2, "one-two", new Busy(), ticketLink("PROJ-1", "start")),
                        linkedNamedSessionView(3, "two-one", new Busy(), ticketLink("PROJ-2", "start")),
                        linkedNamedSessionView(4, "two-two", new Busy(), ticketLink("PROJ-2", "start")))));
        shell.press(key('j'));

        shell.press(alt('k'));
        shell.press(key('j'));
        shell.press(alt('j'));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("two-one");
    }

    @Test
    void shouldShowNothingInTheMainViewForANoSessionRow() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(List.of()));

        assertThat(Screen.of(shell).mainViewRows()).allMatch(String::isEmpty);
    }
}

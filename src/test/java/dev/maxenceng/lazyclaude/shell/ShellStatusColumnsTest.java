package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.unavailableBoardView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewsIn;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketViewFixture.unreadableTicketView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.passedPullRequestView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.newShell;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOn;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnPullRequests;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicketSessions;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.yourTurnOnTheFirstTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.session.published.Busy;

class ShellStatusColumnsTest {

    @Test
    void shouldOpenStatusColumnsInPlaceOfThePanelsWhenZoomingTheBoard() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO));

        shell.press(key('z'));

        assertThat(Screen.of(shell).text())
                .contains("draft (1)", "todo (1)", "in-progress (0)", "in-review (0)", "#1", "#2")
                .doesNotContain("Sessions", "Docs", "main view");
    }

    @Test
    void shouldShowOnlyWhyTheIssuesCannotBeReadWhenZoomingAnUnavailableBoard() {
        Shell shell = shellOn(unavailableBoardView());

        shell.press(key('z'));

        assertThat(Screen.of(shell).text())
                .as("an unavailable board has no status groups, so no empty column stands beside the reason")
                .contains("unavailable", "gh is not logged in")
                .doesNotContain("draft (0)", "todo (0)", "done 0");
    }

    @Test
    void shouldFoldDoneToItsHeaderAndItsCount() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(9, Status.DONE));

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("done 1").doesNotContain("#9");
    }

    @Test
    void shouldShowATicketTheWayItsBoardRowDoes() {
        Shell shell = shellOnTicketSessions(yourTurnOnTheFirstTicket());

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("• #1 ").contains("…");
    }

    @Test
    void shouldShowThePullRequestGlyphOfATicketWhenItFits() {
        Shell shell = shellOnPullRequests(openPullRequestsViewOf(passedPullRequestView()), slugTicketView());

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("✓");
    }

    @Test
    void shouldListEachUnreadableFileWithItsReasonInAColumnAfterDone() {
        Shell shell = shellOn(boardViewOf(List.of(ticketViewNumbered(1, Status.DRAFT)), List.of(unreadableTicketView())));

        shell.press(key('z'));

        String text = Screen.of(shell.render(), 200, 24).text();
        assertThat(text).contains("unreadable (1)", "#2 — ");
        assertThat(text.indexOf("done 0")).isLessThan(text.indexOf("unreadable (1)"));
    }

    @Test
    void shouldShowNoUnreadableColumnWhenEveryFileReads() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).doesNotContain("unreadable");
    }

    @Test
    void shouldOpenEveryColumnEmptyWhenTheBoardHasNoTicket() {
        Shell shell = shellOn();

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("draft (0)", "todo (0)", "in-progress (0)", "in-review (0)", "done 0");
    }

    @Test
    void shouldHighlightTheTicketTheCursorStandsOn() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO));

        shell.press(key('z'));
        shell.press(key('l'));

        assertThat(Screen.of(shell).reversedText()).anySatisfy(text -> assertThat(text).contains("#2"));
    }

    @Test
    void shouldScrollATallColumnToKeepTheCursorVisibleAndSayHowManyRowsRemainBelow() {
        Shell shell = shellOn(ticketViewsIn(Status.TODO, 30));
        shell.press(key('z'));
        assertThat(Screen.of(shell).text()).contains("↓ 12 more");

        pressed(shell, key('j'), 25);

        assertThat(Screen.of(shell).text()).contains("#26 ", "↓ 4 more");
    }

    @Test
    void shouldShowNoMoreLineOnceTheLastRowIsReached() {
        Shell shell = shellOn(ticketViewsIn(Status.TODO, 30));
        shell.press(key('z'));

        pressed(shell, key('j'), 29);

        assertThat(Screen.of(shell).text()).contains("#30 ").doesNotContain("more");
    }

    @Test
    void shouldShortenTitlesOnANarrowTerminalInsteadOfRefusingTheZoom() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        shell.press(key('z'));

        assertThat(Screen.of(shell.render(), 60, 12).text()).contains("#1 ").contains("…");
        assertThat(Screen.of(shell.render(), 40, 12).text()).contains("#1…").doesNotContain("#1 ");
        assertThatCode(() -> Screen.of(shell.render(), 10, 5)).doesNotThrowAnyException();
    }

    @Test
    void shouldFoldTheDockToAStripOfItsTabsAndMarkers() {
        Shell shell = newShell(
                boardViewOf(ticketViewNumbered(1, Status.DRAFT)),
                emptyDocsView(),
                sessionsViewWithTabs(hostedTab(1, "one", new Busy()), hostedTab(2, "two", new Busy())));

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("dock: [∼ one] [∼ two]");
    }

    @Test
    void shouldSayThereIsNoHostedSessionInTheStripWhenTheDockIsEmpty() {
        Shell shell = newShell(boardViewOf(ticketViewNumbered(1, Status.DRAFT)), emptyDocsView(), emptySessionsView());

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("dock: no hosted session");
    }
}

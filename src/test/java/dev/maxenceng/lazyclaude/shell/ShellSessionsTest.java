package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.everyStateSessionViews;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.inputSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.newestSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.oldestSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.sessionViewWith;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.sessionViewWithoutVersion;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.unavailableSessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.waitingSessionView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellFocusedOnMainViewOfSessions;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnSessions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.tamboui.style.Color;

class ShellSessionsTest {

    @Test
    void shouldListEachSessionAsGlyphNameAndAgeFlushRight() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(newestSessionView(), sessionView(), oldestSessionView())));

        assertThat(Screen.of(shell).boardPanelLines())
                .anySatisfy(line -> assertThat(line).matches("∼ lazyclaude-3f\\s+12m"))
                .anySatisfy(line -> assertThat(line).matches("∼ lazyclaude-b6\\s+2h"))
                .anySatisfy(line -> assertThat(line).matches("• claude-tools-a1\\s+1d"));
    }

    @Test
    void shouldKeepTheOrderTheContextGave() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(newestSessionView(), sessionView(), oldestSessionView())));

        assertThat(Screen.of(shell).boardPanelText())
                .containsSubsequence("lazyclaude-3f", "lazyclaude-b6", "claude-tools-a1");
    }

    @Test
    void shouldDrawTheGlyphOfEachStateInItsColour() {
        Screen screen = Screen.of(shellOnSessions(sessionsViewOf(everyStateSessionViews())));

        assertThat(screen.foregroundText(Color.RED)).containsExactly("!");
        assertThat(screen.foregroundText(Color.YELLOW)).containsExactly("?");
        assertThat(screen.foregroundText(Color.GREEN)).containsExactly("∼");
        assertThat(screen.foregroundText(Color.GRAY)).containsExactly("·");
        assertThat(screen.foregroundText(Color.CYAN))
                .as("the focused border is cyan too, so the bullet shows up between its edges")
                .anySatisfy(line -> assertThat(line).contains("•"));
    }

    @Test
    void shouldColourTheGlyphAloneAndNotTheName() {
        Screen screen = Screen.of(shellOnSessions(sessionsViewOf(everyStateSessionViews())));

        assertThat(screen.foregroundText(Color.RED)).doesNotContain("permission-one").containsExactly("!");
    }

    @Test
    void shouldSayThereIsNoLiveSessionInsteadOfStandingEmpty() {
        assertThat(Screen.of(shellOnSessions(emptySessionsView())).dimText()).contains("no live session");
    }

    @Test
    void shouldSayWhyWhenSessionsAreUnavailable() {
        assertThat(String.join(" ", Screen.of(shellOnSessions(unavailableSessionsView())).dimText()))
                .as("the reason wraps inside the panel, so it spans lines")
                .contains("sessions unavailable: no registry and no claude");
    }

    @Test
    void shouldReachThePanelWithAltJFromTheBoardAndLeaveItWithAltK() {
        Shell shell = new Shell(
                boardViewOf(ticketView()),
                emptyDocsView(),
                emptySessionsView());

        shell.press(alt('j'));
        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        shell.press(alt('k'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldReachThePanelWithHFromTheMainView() {
        Shell shell = shellFocusedOnMainViewOfSessions(emptySessionsView());

        shell.press(key('h'));

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
    }

    @Test
    void shouldMoveTheSelectionWithJAndKWhileTheSessionsHaveFocus() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(newestSessionView(), sessionView(), oldestSessionView())));

        pressed(shell, key('j'), 2);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("claude-tools-a1");
        shell.press(key('k'));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("lazyclaude-b6");
    }

    @Test
    void shouldGrowWhileFocusedToShowEverySession() {
        Shell shell = shellOnSessions(sessionsViewOf(everyStateSessionViews()));

        assertThat(Screen.of(shell).boardPanelText()).contains("busy-one", "your-turn-one", "permission-one", "input-one");
    }

    @Test
    void shouldShrinkWhenFocusLeavesForTheBoard() {
        Shell shell = shellOnSessions(sessionsViewOf(everyStateSessionViews()));

        shell.press(alt('k'));

        assertThat(Screen.of(shell).boardPanelText())
                .as("the board grows instead, and the sessions panel keeps its first rows")
                .doesNotContain("input-one");
    }

    @Test
    void shouldListTheMoveKeysWhileTheSessionsHaveFocus() {
        assertThat(Screen.of(shellOnSessions(emptySessionsView())).lines().getLast())
                .contains("j/k move", "h/l panel", "alt+h/j/k/l focus", "q quit")
                .doesNotContain("enter fold");
    }

    @Test
    void shouldShowTheFactsOfTheSelectedSessionInTheMainView() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence(
                        "lazyclaude-b6  ∼ busy",
                        "cwd      /work/app",
                        "pid      17234",
                        "started  2026-10-10 12:00 (2h ago)",
                        "version  2.1.296");
    }

    @Test
    void shouldSayWaitingForPermissionOnAPermissionPrompt() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(waitingSessionView())));

        assertThat(Screen.of(shell).mainViewRows()).contains("lazyclaude-b6  ! waiting for permission");
    }

    @Test
    void shouldSayWhyAWaitingSessionWaitsWhenItIsNotAPermissionPrompt() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(inputSessionView())));

        assertThat(Screen.of(shell).mainViewRows()).contains("lazyclaude-b6  ? waiting for input: input needed");
    }

    @Test
    void shouldSayLazyclaudeDoesNotKnowTheStatusOfASessionInAnUnknownState() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionViewWith(new Unknown()))));

        assertThat(Screen.of(shell).mainViewRows()).contains("lazyclaude-b6  · status unknown to lazyclaude");
    }

    @Test
    void shouldSayYourTurnForASessionThatFinished() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(oldestSessionView())));

        assertThat(Screen.of(shell).mainViewRows()).contains("claude-tools-a1  • your turn");
    }

    @Test
    void shouldLeaveOutTheVersionASourceDoesNotGive() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionViewWithoutVersion())));

        assertThat(Screen.of(shell).mainViewRows()).noneMatch(row -> row.startsWith("version"));
    }

    @Test
    void shouldShowNothingInTheMainViewWithoutASession() {
        assertThat(Screen.of(shellOnSessions(emptySessionsView())).mainViewRows()).allMatch(String::isEmpty);
    }

    @Test
    void shouldFollowTheSelectedSessionWhenAnotherOneStartsAbove() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView(), oldestSessionView())));
        shell.press(key('j'));

        shell.sessionsChanged(sessionsViewOf(List.of(newestSessionView(), sessionView(), oldestSessionView())));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("claude-tools-a1");
        assertThat(Screen.of(shell).boardPanelText()).contains("lazyclaude-3f");
    }

    @Test
    void shouldFollowTheSessionIdThroughAStateChange() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(newestSessionView(), sessionView())));
        shell.press(key('j'));

        shell.sessionsChanged(sessionsViewOf(List.of(newestSessionView(), waitingSessionView())));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("lazyclaude-b6");
        assertThat(Screen.of(shell).mainViewRows()).contains("lazyclaude-b6  ! waiting for permission");
    }

    @Test
    void shouldLandOnTheRowThatTookThePlaceOfASessionThatDied() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(newestSessionView(), sessionView(), oldestSessionView())));
        shell.press(key('j'));

        shell.sessionsChanged(sessionsViewOf(List.of(newestSessionView(), oldestSessionView())));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("claude-tools-a1");
    }

    @Test
    void shouldLandOnTheLastRowWhenTheLastSessionDies() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(newestSessionView(), oldestSessionView())));
        shell.press(key('j'));

        shell.sessionsChanged(sessionsViewOf(List.of(newestSessionView())));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("lazyclaude-3f");
    }

    @Test
    void shouldSelectTheFirstSessionWhenTheFirstOneAppears() {
        Shell shell = shellOnSessions(emptySessionsView());

        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("lazyclaude-b6");
    }

    @Test
    void shouldSayNoLiveSessionAgainWhenTheLastOneDies() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView())));

        shell.sessionsChanged(emptySessionsView());

        assertThat(Screen.of(shell).dimText()).contains("no live session");
    }

    @Test
    void shouldShowTheReasonWhenSessionsBecomeUnavailable() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView())));

        shell.sessionsChanged(unavailableSessionsView());

        assertThat(Screen.of(shell).dimText()).anySatisfy(line -> assertThat(line).contains("sessions unavailable"));
    }

    @Test
    void shouldKeepShowingTheFactsAfterARefreshOfTheSameSession() {
        Shell shell = shellFocusedOnMainViewOfSessions(sessionsViewOf(List.of(sessionView())));

        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).mainViewRows()).contains("pid      17234");
    }
}

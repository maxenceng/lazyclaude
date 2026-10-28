package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewNumbered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewTitled;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewsNumbered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.published.OpeningPromptFixture.openingUnder;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.noTitle;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnSessions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;

class ShellPastSessionsTest {

    @Test
    void shouldListAPastSessionAsADimCircleItsTitleAndTheTimeSinceItsLastActivityFlushRight() {
        Screen screen = Screen.of(shellOnSessions(pastSessionsViewOf(List.of(pastSessionView()))));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("○ Refine PROJ-1\\s+10m"));
        assertThat(screen.dimText()).anySatisfy(line -> assertThat(line).contains("○").doesNotContain("Refine"));
    }

    @Test
    void shouldListPastSessionsAfterTheLiveOnesInTheOrderTheContextGave() {
        Screen screen = Screen.of(shellOnSessions(sessionsViewOf(List.of(sessionView()), pastSessionViewsNumbered(2))));

        assertThat(screen.boardPanelText()).containsSubsequence("lazyclaude-b6", "Past session 1", "Past session 2");
    }

    @Test
    void shouldNotSayThereIsNoLiveSessionWhenOnlyPastOnesExist() {
        assertThat(Screen.of(shellOnSessions(pastSessionsViewOf(List.of(pastSessionView())))).dimText())
                .doesNotContain("no live session");
    }

    @Test
    void shouldSayThereIsNoLiveSessionWhenThereAreNeitherLiveNorPastOnes() {
        assertThat(Screen.of(shellOnSessions(emptySessionsView())).dimText()).contains("no live session");
    }

    @Test
    void shouldShowAllFivePastSessionsAndNoOlderRowWhenThereAreExactlyFive() {
        Screen screen = Screen.of(shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(5))));

        assertThat(screen.boardPanelText()).contains("Past session 5").doesNotContain("older");
    }

    @Test
    void shouldShowTheFiveNewestPastSessionsAndACountOfTheOlderOnesWhenThereAreSix() {
        Screen screen = Screen.of(shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(6))));

        assertThat(screen.boardPanelText()).contains("Past session 5", "… 1 older").doesNotContain("Past session 6");
    }

    @Test
    void shouldCountEveryOlderPastSession() {
        Screen screen = Screen.of(shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(42))));

        assertThat(screen.boardPanelText()).contains("… 37 older");
        assertThat(screen.dimText()).anySatisfy(line -> assertThat(line).contains("… 37 older"));
    }

    @Test
    void shouldCountFiveNewestPastSessionsAfterTheLiveOnesNotInPlaceOfThem() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView()), pastSessionViewsNumbered(6)));
        pressed(shell, key('j'), 6);

        assertThat(Screen.of(shell).boardPanelText()).contains("lazyclaude-b6", "Past session 5", "… 1 older");
    }

    @Test
    void shouldReachTheOlderRowWithJAndUnfoldItInPlaceWithEnter() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(7)));

        pressed(shell, key('j'), 5);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("… 2 older");
        shell.press(enter());
        shell.press(key('j'));

        assertThat(Screen.of(shell).boardPanelText())
                .contains("Past session 5", "Past session 6", "Past session 7")
                .doesNotContain("older");
    }

    @Test
    void shouldLandOnTheFirstOfTheUnfoldedPastSessions() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(7)));
        pressed(shell, key('j'), 5);

        shell.press(enter());

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("Past session 6");
    }

    @Test
    void shouldKeepTheOlderPastSessionsUnfoldedWhenTheSessionsAreReadAgain() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(7)));
        pressed(shell, key('j'), 5);
        shell.press(enter());

        shell.sessionsChanged(pastSessionsViewOf(pastSessionViewsNumbered(7)));
        shell.press(key('j'));

        assertThat(Screen.of(shell).boardPanelText()).contains("Past session 7").doesNotContain("older");
    }

    @Test
    void shouldNotUnfoldAnythingOnEnterOverAPastSession() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(7)));

        shell.press(enter());

        assertThat(Screen.of(shell).boardPanelText()).contains("… 2 older").doesNotContain("Past session 6");
    }

    @Test
    void shouldOfferEnterToExpandOnlyWhileTheOlderRowIsSelected() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(7)));
        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("enter expand");

        pressed(shell, key('j'), 5);

        assertThat(Screen.of(shell).lines().getLast()).contains("enter expand").doesNotContain("enter fold");
    }

    @Test
    void shouldUpdateTheCountOnTheOlderRowWhenAnotherPastSessionAppears() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(7)));
        pressed(shell, key('j'), 5);

        shell.sessionsChanged(pastSessionsViewOf(pastSessionViewsNumbered(8)));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("… 3 older");
    }

    @Test
    void shouldShowNothingInTheMainViewForTheOlderRow() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(7)));

        pressed(shell, key('j'), 5);

        assertThat(Screen.of(shell).mainViewRows()).allMatch(String::isEmpty);
    }

    @Test
    void shouldTitleARowByTheFirstPromptWhenTheSessionHasNoTitle() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionViewTitled(firstPrompt()))));

        assertThat(Screen.of(shell).boardPanelText()).contains("○ /ticket start PROJ-1");
    }

    @Test
    void shouldShowTheShortIdDimmedWhenTheSessionHasNeitherTitleNorPrompt() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionViewTitled(noTitle()))));

        Screen screen = Screen.of(shell);

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).matches("○ 11111111\\s+10m"));
        assertThat(screen.dimText()).anySatisfy(line -> assertThat(line).contains("11111111"));
    }

    @Test
    void shouldFollowTheSelectedPastSessionWhenANewerOneAppearsAbove() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionViewNumbered(2), pastSessionViewNumbered(3))));
        shell.press(key('j'));

        shell.sessionsChanged(pastSessionsViewOf(List.of(pastSessionViewNumbered(1), pastSessionViewNumbered(2), pastSessionViewNumbered(3))));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("Past session 3");
    }

    @Test
    void shouldKeepASessionSelectedWhenItsProcessDiesAndItMovesToThePastBlock() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView()), List.of(pastSessionViewNumbered(2))));

        shell.sessionsChanged(pastSessionsViewOf(List.of(
                pastSessionViewBuilder().id(sessionId()).title(firstPrompt()).opening(openingUnder(firstPrompt())).build(), pastSessionViewNumbered(2))));

        assertThat(Screen.of(shell).reversedText())
                .as("the selection follows the session id from the live block into the past one")
                .singleElement()
                .asString()
                .contains("/ticket start PROJ-1");
    }

    @Test
    void shouldUnfoldTheOlderPastSessionsWhenTheSelectedSessionDiesIntoThem() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView()), pastSessionViewsNumbered(5)));
        List<PastSessionView> past = new ArrayList<>(pastSessionViewsNumbered(5));
        past.add(pastSessionViewBuilder().id(sessionId()).title(firstPrompt()).opening(openingUnder(firstPrompt())).build());

        shell.sessionsChanged(pastSessionsViewOf(past));

        assertThat(Screen.of(shell).reversedText())
                .as("a session idle longer than the five newest past ones dies into the fold, and the selection follows it there")
                .singleElement()
                .asString()
                .contains("/ticket start PROJ-1");
    }

    @Test
    void shouldLandOnTheNeighbourWhenTheSelectedPastSessionIsDeleted() {
        Shell shell = shellOnSessions(pastSessionsViewOf(pastSessionViewsNumbered(3)));
        shell.press(key('j'));

        shell.sessionsChanged(pastSessionsViewOf(List.of(pastSessionViewNumbered(1), pastSessionViewNumbered(3))));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("Past session 3");
    }
}

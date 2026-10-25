package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.tooLargeDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.sharingTheIdPrefixPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.siblingPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.unavailablePullRequestsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.reviewsAnswering;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellOnATicketWith;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellOnATicketWithAnOpenPullRequest;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellReviewing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.tamboui.tui.event.KeyCode;

class ShellReviewTest {

    @Test
    void shouldReplaceTheFourPanelsWithFilesAndCommentsOnPAndKeepTheStatusLineAndTheDock() {
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviewsAnswering(twoFileDiffView(), loadingThreadsView()), Runnable::run);
        String statusLine = Screen.of(shell).statusLine();

        shell.press(key('p'));
        Screen screen = Screen.of(shell);

        assertThat(screen.text()).contains("╭Files", "╭Comments", "╭dock").doesNotContain("╭Board", "╭Sessions", "╭Docs", "╭Elsewhere");
        assertThat(screen.statusLine()).isEqualTo(statusLine);
    }

    @Test
    void shouldShowThePullRequestsNumberAndTitleAsTheMainViewsTitleAndItsDiffInside() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), loadingThreadsView()));

        assertThat(screen.text()).contains("PR #23 PR and CI glyphs");
        assertThat(screen.mainViewRows()).anySatisfy(row -> assertThat(row).startsWith("── src/Board.java"));
    }

    @Test
    void shouldRestoreTheFourPanelsWithTheSameTicketSelectedOnEscape() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());

        shell.press(namedKey(KeyCode.ESCAPE));
        Screen screen = Screen.of(shell);

        assertThat(screen.text()).contains("╭Board", "╭Sessions", "╭Docs", "╭Elsewhere").doesNotContain("╭Files", "╭Comments");
        assertThat(screen.reversedText()).anySatisfy(row -> assertThat(row).contains("PROJ-1"));
        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldLeaveOnlyTheZoomOnEscapeInAZoomedDiffAndTheReviewOnTheNextEscape() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());
        shell.press(key('z'));
        assertThat(Screen.of(shell).text()).doesNotContain("╭Files");

        shell.press(namedKey(KeyCode.ESCAPE));
        assertThat(Screen.of(shell).text()).as("esc steps back out of the zoom before it leaves the review").contains("╭Files", "╭Comments");
        assertThat(shell.focus()).isEqualTo(Focus.MAIN);

        shell.press(namedKey(KeyCode.ESCAPE));
        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("╭Files");
    }

    @Test
    void shouldKeepTheReviewsKeysInTheKeyBarOfAZoomedDiffAndAddZBack() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());

        shell.press(key('z'));

        assertThat(Screen.of(shell).lines().getLast())
                .contains("c comment  v range  enter edit", "z back")
                .doesNotContain("h/l panel", "j/k scroll");
    }

    @Test
    void shouldListPReviewInTheKeyBarOnATicketWhoseTicketBranchHeadsAnOpenPullRequest() {
        Screen screen = Screen.of(shellOnATicketWithAnOpenPullRequest(reviewsAnswering(twoFileDiffView(), loadingThreadsView()), Runnable::run));

        assertThat(screen.lines().getLast()).contains("p review");
    }

    @Test
    void shouldListNoPReviewAndDoNothingOnPWhenThereIsNoOpenPullRequestForTheTicket() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWith(openPullRequestsViewOf(siblingPullRequestView()), reviews, Runnable::run);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("p review");
        shell.press(key('p'));

        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("╭Files");
        verify(reviews, never()).diffOf(any());
    }

    @Test
    void shouldListNoPReviewAndDoNothingOnPWhenThePullRequestsHeadOnlySharesTheIdPrefix() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWith(openPullRequestsViewOf(sharingTheIdPrefixPullRequestView()), reviews, Runnable::run);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("p review");
        shell.press(key('p'));

        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("╭Files");
        verify(reviews, never()).diffOf(any());
    }

    @Test
    void shouldListNoPReviewAndDoNothingOnPWhileTheOpenPullRequestFetchIsChecking() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWith(checkingPullRequestsView(), reviews, Runnable::run);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("p review");
        shell.press(key('p'));

        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("╭Files");
        verify(reviews, never()).diffOf(any());
    }

    @Test
    void shouldListNoPReviewAndDoNothingOnPWhenTheOpenPullRequestsAreUnknown() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWith(unavailablePullRequestsView(), reviews, Runnable::run);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("p review");
        shell.press(key('p'));

        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("╭Files");
        verify(reviews, never()).diffOf(any());
    }

    @Test
    void shouldShowLoadingDimInTheMainViewWhileTheReadsAreUnderWay() {
        HeldTasksFixture held = new HeldTasksFixture();
        Shell shell = shellOnATicketWith(
                openPullRequestsViewOf(pullRequestView()), reviewsAnswering(twoFileDiffView(), loadingThreadsView()), held);

        shell.press(key('p'));
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows()).contains("loading…");
        assertThat(screen.dimText()).contains("loading…");
        held.runNewestFirst();
        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).startsWith("── src/Board.java"));
    }

    @Test
    void shouldReadTheDiffAndThreadsOnceOnPAndNotAgainOnRendersOrKeys() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run);

        shell.press(key('p'));
        Screen.of(shell);
        shell.press(key('j'));
        Screen.of(shell);

        verify(reviews, times(1)).diffOf(any());
        verify(reviews, times(1)).threadsOf(any());
    }

    @Test
    void shouldReadBothAgainOnRefresh() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run);
        shell.press(key('p'));

        shell.press(key('R'));

        verify(reviews, times(2)).diffOf(any());
        verify(reviews, times(2)).threadsOf(any());
    }

    @Test
    void shouldShowTheNewestReadWhenAnOlderOneLandsAfterIt() {
        ReviewQuery reviews = reviewsAnswering(unavailableDiffView(), loadingThreadsView());
        when(reviews.diffOf(any())).thenReturn(twoFileDiffView(), unavailableDiffView());
        HeldTasksFixture held = new HeldTasksFixture();
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, held);
        shell.press(key('p'));
        shell.press(key('R'));

        held.runNewestFirst();

        assertThat(Screen.of(shell).mainViewRows())
                .as("a read begun earlier must not overwrite the answer of the one begun later")
                .anySatisfy(row -> assertThat(row).startsWith("── src/Board.java"));
    }

    @Test
    void shouldDropAReadThatLandsAfterTheReviewWasLeft() {
        HeldTasksFixture held = new HeldTasksFixture();
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviewsAnswering(twoFileDiffView(), loadingThreadsView()), held);
        shell.press(key('p'));
        shell.press(namedKey(KeyCode.ESCAPE));

        held.runNewestFirst();

        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("╭Files", "src/Board.java");
    }

    @Test
    void shouldDropAReadThatLandsInAReviewOpenedAfterwards() {
        HeldTasksFixture held = new HeldTasksFixture();
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, held);
        shell.press(key('p'));
        shell.press(namedKey(KeyCode.ESCAPE));
        shell.press(key('p'));
        when(reviews.diffOf(any())).thenReturn(unavailableDiffView());

        held.runNewestFirst();

        assertThat(Screen.of(shell).mainViewRows()).contains("PR unknown (gh: not logged in)");
    }

    @Test
    void shouldShowPrUnknownWithTheReasonAndAnEmptyFilesPanelWhenTheDiffCannotBeRead() {
        Screen screen = Screen.of(shellReviewing(unavailableDiffView(), loadingThreadsView()));

        assertThat(screen.mainViewRows()).contains("PR unknown (gh: not logged in)");
        assertThat(screen.dimText()).contains("PR unknown (gh: not logged in)");
        assertThat(screen.boardPanelLines()).noneSatisfy(row -> assertThat(row).contains("Board.java"));
    }

    @Test
    void shouldShowDiffTooLargeForGitHubWithTheReasonWhenGitHubRefusesTheDiff() {
        Screen screen = Screen.of(shellReviewing(tooLargeDiffView(), loadingThreadsView()));

        assertThat(screen.mainViewRows()).contains("diff too large for GitHub (gh: not logged in)");
    }

    @Test
    void shouldReachNeitherTheStatusLineNorTheKeysWhenTheReadsFailAndStillLeaveOnEscape() {
        Shell shell = shellReviewing(unavailableDiffView(), unavailableThreadsView());
        String before = Screen.of(shellOnATicketWithAnOpenPullRequest(reviewsAnswering(twoFileDiffView(), loadingThreadsView()), Runnable::run)).statusLine();

        assertThat(Screen.of(shell).statusLine()).isEqualTo(before);
        shell.press(namedKey(KeyCode.ESCAPE));
        assertThat(Screen.of(shell).text()).contains("╭Board");
    }

    @Test
    void shouldReplaceTheDiffWithTheReasonWhenARefreshFailsAfterOneSucceeded() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run);
        shell.press(key('p'));
        when(reviews.diffOf(any())).thenReturn(unavailableDiffView());

        shell.press(key('R'));
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows()).contains("PR unknown (gh: not logged in)");
        assertThat(screen.text()).as("the last good diff is never shown as current").doesNotContain("src/Board.java");
    }

    @Test
    void shouldStillShowLoadingForTheDiffWhenOnlyTheThreadsHaveAnswered() {
        assertThat(Screen.of(shellReviewing(loadingDiffView(), unavailableThreadsView())).mainViewRows()).contains("loading…");
    }
}

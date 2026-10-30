package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.labelledSlugTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.draftPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.failedPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.shortTitledDraftPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.siblingPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.unavailablePullRequestsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnPullRequests;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShellPullRequestDetailTest {

    @Test
    void shouldShowThePullRequestsNumberAndTitleUnderTheFacts() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(pullRequestView()), slugTicketView()));

        assertThat(screen.mainViewRows())
                .containsSubsequence("#1 · draft · board · feature", "PR      #23 PR and CI glyphs on the board");
    }

    @Test
    void shouldSayWhenThePullRequestIsADraftPullRequest() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(shortTitledDraftPullRequestView()), slugTicketView()));

        assertThat(screen.mainViewRows()).contains("PR      #23 Board glyphs (draft PR)");
    }

    @Test
    void shouldCutALongTitleShortRatherThanTheDraftSuffix() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(draftPullRequestView()), slugTicketView()));

        assertThat(screen.mainViewRows())
                .anySatisfy(row -> assertThat(row).startsWith("PR      #23 PR and CI").contains("…").endsWith(" (draft PR)").hasSize(50));
    }

    @Test
    void shouldListTheChecksFailedFirstThenPendingThenPassedUnderTheirKey() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(failedPullRequestView()), slugTicketView()));

        assertThat(screen.mainViewRows())
                .containsSubsequence("checks  ✗ build", "        ◌ contract", "        ✓ lint");
    }

    @Test
    void shouldShowNoChecksRowForAPullRequestWithoutChecks() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(pullRequestView()), slugTicketView()));

        assertThat(screen.mainViewRows()).noneSatisfy(row -> assertThat(row).startsWith("checks"));
    }

    @Test
    void shouldPutThePullRequestRowsBetweenTheFrontmatterRowsAndTheRule() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(failedPullRequestView()), labelledSlugTicketView()));

        assertThat(screen.mainViewRows())
                .containsSubsequence("labels  bug, help wanted", "PR      #23 PR and CI glyphs on the board", "checks  ✗ build", "─".repeat(50));
    }

    @Test
    void shouldSayInDimTextThereIsNoPullRequestForATicketWithNone() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(siblingPullRequestView()), slugTicketView()));

        assertThat(screen.mainViewRows()).contains("PR      none");
        assertThat(screen.dimText()).contains("PR      none");
    }

    @Test
    void shouldSayInDimTextThePullRequestsAreBeingCheckedBeforeTheFirstFetchAnswers() {
        Screen screen = Screen.of(shellOnPullRequests(checkingPullRequestsView(), slugTicketView()));

        assertThat(screen.mainViewRows()).contains("PR      checking…");
        assertThat(screen.dimText()).contains("PR      checking…");
    }

    @Test
    void shouldSayInDimTextWhyGitHubCannotBeRead() {
        Screen screen = Screen.of(shellOnPullRequests(unavailablePullRequestsView(), slugTicketView()));

        assertThat(screen.mainViewRows()).contains("PR      unknown (gh: not logged in)");
        assertThat(screen.dimText()).contains("PR      unknown (gh: not logged in)");
    }

    @Test
    void shouldShowTheReasonInsteadOfTheLastGoodResultWhenALaterFetchFails() {
        Shell shell = shellOnPullRequests(openPullRequestsViewOf(failedPullRequestView()), slugTicketView());
        shell.pullRequestsChanged(unavailablePullRequestsView());

        assertThat(Screen.of(shell).mainViewRows())
                .contains("PR      unknown (gh: not logged in)")
                .noneSatisfy(row -> assertThat(row).contains("✗ build"));
    }

    @Test
    void shouldFollowTheFetchWhileTheTicketIsOpen() {
        Shell shell = shellOnPullRequests(checkingPullRequestsView(), slugTicketView());
        Screen.of(shell);
        shell.pullRequestsChanged(openPullRequestsViewOf(pullRequestView()));

        assertThat(Screen.of(shell).mainViewRows()).contains("PR      #23 PR and CI glyphs on the board");
    }
}

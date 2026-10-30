package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.inReviewSlugTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longTitledSlugTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.bareIdPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.draftFailedPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.draftPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.failedPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.longerHeadPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.passedPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pendingPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.sharingTheIdPrefixPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.siblingPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.unavailablePullRequestsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnPullRequests;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnPullRequestsAndSessions;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.yourTurnOnTheFirstTicket;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;

class ShellPullRequestsTest {

    @Test
    void shouldShowNoGlyphBeforeTheFirstFetchHasAnswered() {
        Screen screen = Screen.of(shellOnPullRequests(checkingPullRequestsView(), slugTicketView()));

        assertThat(screen.boardPanelText()).doesNotContain("✓", "✗", "◌", "⊙");
    }

    @Test
    void shouldShowAGreenTickAtTheRightEdgeOfTheRowForAPassedCiResult() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(passedPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).startsWith("  #1 ").hasSize(26).endsWith(" ✓"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("#1 A board"), Color.GREEN)).isEqualTo("✓");
    }

    @Test
    void shouldShowARedCrossForAFailedCiResult() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(failedPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).endsWith(" ✗"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("#1 A board"), Color.RED)).isEqualTo("✗");
    }

    @Test
    void shouldShowAYellowRingForAPendingCiResult() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(pendingPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).endsWith(" ◌"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("#1 A board"), Color.YELLOW)).isEqualTo("◌");
    }

    @Test
    void shouldShowADimMarkBesideTheCiGlyphOfADraftPullRequest() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(draftFailedPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).hasSize(26).endsWith("⊙✗"));
        assertThat(screen.dimText()).contains("⊙");
    }

    @Test
    void shouldShowOnlyTheDimMarkForAnOpenPullRequestWithNoChecks() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(pullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).hasSize(25).endsWith("⊙"));
        assertThat(screen.dimText()).contains("⊙");
    }

    @Test
    void shouldShowOnlyTheDimMarkForADraftPullRequestWithNoChecks() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(draftPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).hasSize(25).endsWith("⊙"));
    }

    @Test
    void shouldShowTheGlyphWhateverTheTicketsStatus() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(passedPullRequestView()), inReviewSlugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).startsWith("  #1 ").endsWith("✓"));
    }

    @Test
    void shouldShowNoGlyphForAPullRequestWhoseHeadOnlySharesTheIdPrefix() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(sharingTheIdPrefixPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelText()).as("PROJ-10-… is another ticket's branch").doesNotContain("✓", "⊙");
    }

    @Test
    void shouldShowNoGlyphForAPullRequestWhoseHeadIsTheBareId() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(bareIdPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelText()).doesNotContain("✓", "⊙");
    }

    @Test
    void shouldShowNoGlyphForAPullRequestWhoseHeadMerelyStartsWithTheTicketBranch() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(longerHeadPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelText()).doesNotContain("✓", "⊙");
    }

    @Test
    void shouldShowNoGlyphForATicketWhosePullRequestIsAnotherTicketsOwn() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(siblingPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelText()).doesNotContain("✓");
    }

    @Test
    void shouldShowNoGlyphWhenGitHubCannotBeRead() {
        Screen screen = Screen.of(shellOnPullRequests(unavailablePullRequestsView(), slugTicketView()));

        assertThat(screen.boardPanelText()).doesNotContain("✓", "✗", "◌", "⊙");
    }

    @Test
    void shouldClearTheGlyphsWhenALaterFetchFailsRatherThanKeepTheLastGoodResult() {
        Shell shell = shellOnPullRequests(openPullRequestsViewOf(passedPullRequestView()), slugTicketView());
        shell.pullRequestsChanged(unavailablePullRequestsView());

        assertThat(Screen.of(shell).boardPanelText())
                .as("the last good result must never be shown as current")
                .doesNotContain("✓");
    }

    @Test
    void shouldShowTheGlyphsOnceAFetchAnswersAfterTheBoardWasDrawn() {
        Shell shell = shellOnPullRequests(checkingPullRequestsView(), slugTicketView());
        Screen.of(shell);
        shell.pullRequestsChanged(openPullRequestsViewOf(failedPullRequestView()));

        assertThat(Screen.of(shell).boardPanelText()).contains("✗");
    }

    @Test
    void shouldTruncateALongTitleBeforeTheGlyphs() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(draftFailedPullRequestView()), longTitledSlugTicketView()));

        assertThat(screen.boardPanelLines())
                .anySatisfy(line -> assertThat(line).startsWith("  #1 PR and CI").contains("…").hasSize(26).endsWith("⊙✗"));
    }

    @Test
    void shouldLeaveTheSessionMarkerInTheGutterBesideTheGlyphs() {
        Screen screen = Screen.of(shellOnPullRequestsAndSessions(
                openPullRequestsViewOf(failedPullRequestView()),
                yourTurnOnTheFirstTicket(),
                slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).startsWith("• #1 ").endsWith("✗"));
    }

    @Test
    void shouldNotChangeTheStatusLineWhateverTheCiResultIs() {
        String without = Screen.of(shellOnPullRequestsAndSessions(
                        checkingPullRequestsView(), yourTurnOnTheFirstTicket(), slugTicketView()))
                .statusLine();
        String withFailure = Screen.of(shellOnPullRequestsAndSessions(
                        openPullRequestsViewOf(failedPullRequestView()), yourTurnOnTheFirstTicket(), slugTicketView()))
                .statusLine();

        assertThat(withFailure).as("a CI result is not a marker").isEqualTo(without);
    }

    @Test
    void shouldGiveAGroupHeaderNoGlyphColumn() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(passedPullRequestView()), slugTicketView()));

        assertThat(screen.boardPanelLines()).anySatisfy(line -> assertThat(line).startsWith("▾ draft").doesNotContain("✓"));
    }

    @Test
    void shouldReverseTheGlyphsOfTheSelectedRowWithTheRestOfIt() {
        Screen screen = Screen.of(shellOnPullRequests(openPullRequestsViewOf(draftFailedPullRequestView()), slugTicketView()));

        assertThat(screen.reversedText()).anySatisfy(text -> assertThat(text).contains("#1").endsWith("⊙✗"));
    }
}

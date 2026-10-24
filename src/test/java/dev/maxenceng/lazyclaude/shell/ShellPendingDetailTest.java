package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.unavailablePendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.pendingOnBothFiles;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.reviewsAnswering;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellOnATicketWithAnOpenPullRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.tamboui.tui.event.KeyCode;

class ShellPendingDetailTest {

    @Test
    void shouldShowThePendingCountUnderThePullRequestHeadingOnceTheSelectionLandedOnTheTicket() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView(), pendingOnBothFiles());
        Screen screen = Screen.of(shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run));

        assertThat(screen.mainViewRows()).containsSubsequence("PR      #23 PR and CI glyphs on the board", "        ✎3 pending");
        assertThat(screen.foregroundText(PendingText.COLOUR)).anySatisfy(text -> assertThat(text).contains("✎3 pending"));
    }

    @Test
    void shouldShowNothingWhenThereIsNoPendingReview() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView(), noPendingReviewView());

        assertThat(Screen.of(shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run)).mainViewRows())
                .noneSatisfy(row -> assertThat(row).contains("pending"));
    }

    @Test
    void shouldShowWhyDimWhenThePendingReviewCannotBeRead() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView(), unavailablePendingView());
        Screen screen = Screen.of(shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run));

        assertThat(screen.mainViewRows()).anySatisfy(row -> assertThat(row).contains("pending unknown (gh: not logged in)"));
        assertThat(screen.dimText()).anySatisfy(text -> assertThat(text).contains("pending unknown"));
    }

    @Test
    void shouldReadOnceOnLandingAgainOnRefreshAndAgainWhenTheReviewIsLeft() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView(), pendingOnBothFiles());
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run);
        Screen.of(shell);
        Screen.of(shell);
        verify(reviews, times(1)).pendingOf(any());

        shell.press(key('R'));
        Screen.of(shell);
        verify(reviews, times(2)).pendingOf(any());

        shell.press(key('p'));
        verify(reviews, times(3)).pendingOf(any());
        shell.press(namedKey(KeyCode.ESCAPE));
        Screen.of(shell);
        verify(reviews, times(4)).pendingOf(any());
    }

    @Test
    void shouldReadOffTheRenderThreadAndShowNothingUntilTheReadLands() {
        HoldableTasksFixture background = new HoldableTasksFixture();
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView(), pendingOnBothFiles());
        background.startHolding();
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, background);

        assertThat(Screen.of(shell).mainViewRows()).noneSatisfy(row -> assertThat(row).contains("pending"));
        background.releaseInOrder();

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("✎3 pending"));
    }

    @Test
    void shouldDropAPendingReadThatLandsAfterANewerOneWasBegun() {
        HeldTasksFixture background = new HeldTasksFixture();
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView(), noPendingReviewView());
        when(reviews.pendingOf(any())).thenReturn(pendingOnBothFiles(), unavailablePendingView());
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, background);
        Screen.of(shell);
        shell.press(key('R'));
        Screen.of(shell);

        background.runNewestFirst();

        assertThat(Screen.of(shell).mainViewRows())
                .as("the older read answers last, and its answer must not replace the newer one")
                .anySatisfy(row -> assertThat(row).contains("✎3 pending"))
                .noneSatisfy(row -> assertThat(row).contains("pending unknown"));
    }
}

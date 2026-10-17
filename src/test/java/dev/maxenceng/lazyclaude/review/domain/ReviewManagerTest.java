package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.knownMergedPullRequests;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.openPullRequests;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.unavailableMergedPullRequests;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.unavailablePullRequests;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.watchedRefresh;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewManagerTest {

    @Mock
    private OpenPullRequestPort pullRequests;

    @Mock
    private MergedPullRequestPort mergedPullRequests;

    @Mock
    private PullRequestWatchPort watch;

    private ReviewManager manager;
    private final List<PullRequests> delivered = new ArrayList<>();
    private final List<MergedPullRequests> mergedDelivered = new ArrayList<>();

    @BeforeEach
    void createManager() {
        manager = new ReviewManager(pullRequests, mergedPullRequests, watch);
    }

    @Test
    void shouldBeCheckingBeforeTheFirstFetchAnswers() {
        assertThat(manager.checking()).isEqualTo(new CheckingPullRequests());
    }

    @Test
    void shouldDeliverTheOpenPullRequestsOnEachRunOfTheWatch() {
        when(pullRequests.openPullRequests(project())).thenReturn(openPullRequests().pullRequests());
        manager.watch(project(), delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(openPullRequests());
    }

    @Test
    void shouldDeliverWhyGitHubCannotBeReadInsteadOfFailing() {
        when(pullRequests.openPullRequests(project())).thenThrow(pullRequestsUnavailable());
        manager.watch(project(), delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(unavailablePullRequests());
    }

    @Test
    void shouldNotKeepTheLastGoodResultWhenALaterFetchFails() {
        when(pullRequests.openPullRequests(project()))
                .thenReturn(openPullRequests().pullRequests())
                .thenThrow(pullRequestsUnavailable())
                .thenReturn(openPullRequests().pullRequests());
        manager.watch(project(), delivered::add, mergedDelivered::add);
        Runnable refresh = watchedRefresh(watch);

        refresh.run();
        refresh.run();
        refresh.run();

        assertThat(delivered)
                .as("the failed run must replace the good one, not leave it standing as current")
                .containsExactly(openPullRequests(), unavailablePullRequests(), openPullRequests());
    }

    @Test
    void shouldNotFetchUntilTheWatchRuns() {
        manager.watch(project(), delivered::add, mergedDelivered::add);

        verifyNoInteractions(pullRequests);
    }

    @Test
    void shouldDeliverTheMergedPullRequestsOnEachRunOfTheWatch() {
        when(mergedPullRequests.mergedPullRequests(project())).thenReturn(knownMergedPullRequests().pullRequests());
        manager.watch(project(), delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(mergedDelivered).containsExactly(knownMergedPullRequests());
    }

    @Test
    void shouldDeliverWhyTheMergedOnesCannotBeReadInsteadOfFailing() {
        when(mergedPullRequests.mergedPullRequests(project())).thenThrow(pullRequestsUnavailable());
        manager.watch(project(), delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(mergedDelivered).containsExactly(unavailableMergedPullRequests());
    }

    @Test
    void shouldNotKeepTheLastGoodMergedResultWhenALaterFetchFails() {
        when(mergedPullRequests.mergedPullRequests(project()))
                .thenReturn(knownMergedPullRequests().pullRequests())
                .thenThrow(pullRequestsUnavailable());
        manager.watch(project(), delivered::add, mergedDelivered::add);
        Runnable refresh = watchedRefresh(watch);

        refresh.run();
        refresh.run();

        assertThat(mergedDelivered)
                .as("branches count as sources again while GitHub cannot be read")
                .containsExactly(knownMergedPullRequests(), unavailableMergedPullRequests());
    }

    @Test
    void shouldStillDeliverTheMergedOnesWhenTheOpenOnesCannotBeRead() {
        when(pullRequests.openPullRequests(project())).thenThrow(pullRequestsUnavailable());
        when(mergedPullRequests.mergedPullRequests(project())).thenReturn(knownMergedPullRequests().pullRequests());
        manager.watch(project(), delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(unavailablePullRequests());
        assertThat(mergedDelivered).containsExactly(knownMergedPullRequests());
    }

    @Test
    void shouldWatchOnTheOneTimerSoTheTwoFetchesNeverOverlap() {
        manager.watch(project(), delivered::add, mergedDelivered::add);

        verify(watch, times(1)).watch(any());
    }

    @Test
    void shouldRunTheWatchNowWhenRefreshed() {
        manager.runNow();

        verify(watch).runNow();
    }
}

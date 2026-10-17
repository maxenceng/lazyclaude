package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.domain.MergedPullRequestFixture.mergedPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.draftPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.failingPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.pendingPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.pullRequest;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.watchedRefresh;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedHeadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.noMergedPullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.siblingMergedHeadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.draftPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.failedPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pendingPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.unavailablePullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQueryFixture.reviewQuery;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.sharingTheIdPrefix;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.siblingHeadBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.secondHeadCommit;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;
import dev.maxenceng.lazyclaude.review.published.NoMergedHead;

@ExtendWith(MockitoExtension.class)
class ReviewQueryTest {

    private static final ReviewPayload OF_THE_PROJECT = new ReviewPayload(project().root());

    @Mock
    private OpenPullRequestPort pullRequests;

    @Mock
    private MergedPullRequestPort mergedPullRequests;

    @Mock
    private PullRequestWatchPort watch;

    private final List<PullRequestsView> delivered = new ArrayList<>();
    private final List<MergedPullRequestsView> mergedDelivered = new ArrayList<>();
    private ReviewQuery query;

    @BeforeEach
    void createQuery() {
        query = reviewQuery(pullRequests, mergedPullRequests, watch);
    }

    @Test
    void shouldViewCheckingBeforeTheFirstFetchAnswers() {
        assertThat(query.checking()).isEqualTo(checkingPullRequestsView());
    }

    @Test
    void shouldViewAPullRequestWithNoChecksAndNoCiResult() {
        when(pullRequests.openPullRequests(project())).thenReturn(List.of(pullRequest()));
        query.subscribe(OF_THE_PROJECT, delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(openPullRequestsViewOf(pullRequestView()));
    }

    @Test
    void shouldViewAFailedCiResultWithItsChecksFailedFirst() {
        when(pullRequests.openPullRequests(project())).thenReturn(List.of(failingPullRequest()));
        query.subscribe(OF_THE_PROJECT, delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(openPullRequestsViewOf(failedPullRequestView()));
    }

    @Test
    void shouldViewAPendingCiResult() {
        when(pullRequests.openPullRequests(project())).thenReturn(List.of(pendingPullRequest()));
        query.subscribe(OF_THE_PROJECT, delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(openPullRequestsViewOf(pendingPullRequestView()));
    }

    @Test
    void shouldViewADraftPullRequestAsDraft() {
        when(pullRequests.openPullRequests(project())).thenReturn(List.of(draftPullRequest()));
        query.subscribe(OF_THE_PROJECT, delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(openPullRequestsViewOf(draftPullRequestView()));
    }

    @Test
    void shouldViewWhyGitHubCannotBeRead() {
        when(pullRequests.openPullRequests(project())).thenThrow(pullRequestsUnavailable());
        query.subscribe(OF_THE_PROJECT, delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(unavailablePullRequestsView());
    }

    @Test
    void shouldViewTheMergedPullRequestsByTheirHeadBranch() {
        when(mergedPullRequests.mergedPullRequests(project())).thenReturn(List.of(mergedPullRequest()));
        query.subscribe(OF_THE_PROJECT, delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(mergedDelivered).containsExactly(mergedPullRequestsViewOf(mergedHeadView()));
    }

    @Test
    void shouldViewTheHeadBranchesOfTheMergedPullRequestsAsPlainNames() {
        assertThat(mergedPullRequestsViewOf(mergedHeadView(), siblingMergedHeadView()).headBranches())
                .containsExactlyInAnyOrder("PROJ-1-first-slice", "PROJ-2");
    }

    @Test
    void shouldViewTheMergedHeadOfABranchByItsName() {
        assertThat(mergedPullRequestsViewOf(mergedHeadView(), siblingMergedHeadView()).mergedHeadOf(siblingHeadBranch()))
                .isEqualTo(secondHeadCommit());
    }

    @Test
    void shouldViewNoMergedHeadForABranchNoMergedPullRequestHas() {
        assertThat(mergedPullRequestsViewOf(mergedHeadView()).mergedHeadOf(sharingTheIdPrefix()))
                .isEqualTo(new NoMergedHead());
    }

    @Test
    void shouldViewNoMergedPullRequestsWhileGitHubCannotBeRead() {
        when(mergedPullRequests.mergedPullRequests(project())).thenThrow(pullRequestsUnavailable());
        query.subscribe(OF_THE_PROJECT, delivered::add, mergedDelivered::add);

        watchedRefresh(watch).run();

        assertThat(mergedDelivered)
                .as("a branch counts as a source again, and nothing is raised about it")
                .containsExactly(noMergedPullRequestsView());
    }

    @Test
    void shouldRunTheWatchNowWhenRefreshed() {
        query.refresh();

        verify(watch).runNow();
    }
}

package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.noMergedPullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.loadingPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.unavailablePendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.pullRequestPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.unavailablePullRequestsView;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.anchorParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.checking;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.loadingDiff;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.loadingDiffAndPendingParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.loadingDiffAndThreadsParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.loadingPending;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.loadingThreads;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.mergedAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.mergedChanged;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.noneInDiff;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.otherReviewParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.pullRequestParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.pullRequestsAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.refusedAnchor;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.reviewChanged;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.reviewParams;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.unavailableDiff;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.unavailablePending;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.unavailablePullRequests;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.unavailableThreads;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.failingAfterReleasing;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.projectPath;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.waitingFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.CommentAnchorPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.RefusedAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffView;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;

@ExtendWith(MockitoExtension.class)
class ReviewSidecarTest {

    @Mock
    private ReviewQuery review;

    @Test
    void answers_what_stands_until_the_diff_is_read() {
        when(review.loadingDiff()).thenReturn(loadingDiffView());

        assertThat(linesWritten(review, request(1, "review/loadingDiff", "{}"))).containsExactly(ready(), resultOf(1, loadingDiff()));
    }

    @Test
    void answers_what_stands_until_the_threads_are_read() {
        when(review.loadingThreads()).thenReturn(loadingThreadsView());

        assertThat(linesWritten(review, request(1, "review/loadingThreads", "{}"))).containsExactly(ready(), resultOf(1, loadingThreads()));
    }

    @Test
    void answers_what_stands_until_the_pending_review_is_read() {
        when(review.loadingPending()).thenReturn(loadingPendingView());

        assertThat(linesWritten(review, request(1, "review/loadingPending", "{}"))).containsExactly(ready(), resultOf(1, loadingPending()));
    }

    @Test
    void answers_what_stands_until_the_first_fetch_answers() {
        when(review.checking()).thenReturn(checkingPullRequestsView());

        assertThat(linesWritten(review, request(1, "review/checking", "{}"))).containsExactly(ready(), resultOf(1, checking()));
    }

    @Test
    void answers_the_diff_of_a_pull_request_with_why_it_could_not_be_read_as_a_result() {
        when(review.diffOf(pullRequestPayload(projectPath(), pullRequestNumber()))).thenReturn(unavailableDiffView());

        assertThat(linesWritten(review, request(1, "review/diffOf", pullRequestParams())))
                .containsExactly(ready(), resultOf(1, unavailableDiff()));
    }

    @Test
    void answers_the_threads_of_a_pull_request_with_why_they_could_not_be_read_as_a_result() {
        when(review.threadsOf(pullRequestPayload(projectPath(), pullRequestNumber()))).thenReturn(unavailableThreadsView());

        assertThat(linesWritten(review, request(1, "review/threadsOf", pullRequestParams())))
                .containsExactly(ready(), resultOf(1, unavailableThreads()));
    }

    @Test
    void answers_the_pending_review_of_a_pull_request_with_why_it_could_not_be_read_as_a_result() {
        when(review.pendingOf(pullRequestPayload(projectPath(), pullRequestNumber()))).thenReturn(unavailablePendingView());

        assertThat(linesWritten(review, request(1, "review/pendingOf", pullRequestParams())))
                .containsExactly(ready(), resultOf(1, unavailablePending()));
    }

    @Test
    void joins_the_diff_and_the_threads_it_is_sent() {
        when(review.threadsInDiff(new ThreadsInDiffPayload(loadingDiffView(), loadingThreadsView())))
                .thenReturn(new ThreadsInDiffView(List.of()));

        assertThat(linesWritten(review, request(1, "review/threadsInDiff", loadingDiffAndThreadsParams())))
                .containsExactly(ready(), resultOf(1, noneInDiff()));
    }

    @Test
    void joins_the_diff_and_the_pending_review_it_is_sent() {
        when(review.pendingInDiff(new PendingCommentsInDiffPayload(loadingDiffView(), loadingPendingView())))
                .thenReturn(new PendingCommentsInDiffView(List.of()));

        assertThat(linesWritten(review, request(1, "review/pendingInDiff", loadingDiffAndPendingParams())))
                .containsExactly(ready(), resultOf(1, noneInDiff()));
    }

    @Test
    void answers_a_refused_anchor_as_a_result() {
        when(review.anchorOf(new CommentAnchorPayload(loadingDiffView(), new DiffLineIndex(2), new DiffLineIndex(3))))
                .thenReturn(new RefusedAnchorView(reason()));

        assertThat(linesWritten(review, request(1, "review/anchorOf", anchorParams())))
                .as("lines that take no comment are an outcome the frontend shows, not a failure of the call")
                .containsExactly(ready(), resultOf(1, refusedAnchor()));
    }

    @Test
    void fetches_again_now_and_answers_null() {
        assertThat(linesWritten(review, request(1, "review/refresh", "{}"))).containsExactly(ready(), resultOf(1, "null"));
        verify(review).refresh();
    }

    @Test
    void answers_the_pull_requests_checked_so_far_to_a_watch() {
        when(review.checking()).thenReturn(checkingPullRequestsView());

        assertThat(linesWritten(review, request(1, "review/watch", reviewParams()))).containsExactly(ready(), resultOf(1, checking()));
    }

    @Test
    void pushes_the_next_open_pull_requests_after_the_answer() {
        when(review.checking()).thenReturn(checkingPullRequestsView());
        doAnswer(pullRequestsAtOnce(unavailablePullRequestsView())).when(review).subscribe(any(), any(), any());

        assertThat(linesWritten(review, request(1, "review/watch", reviewParams())))
                .as("a change pushed before the watch's answer would be overwritten by the older pull requests it carries")
                .containsExactly(ready(), resultOf(1, checking()), reviewChanged(unavailablePullRequests()));
    }

    @Test
    void pushes_the_merged_pull_requests_apart_from_the_open_ones() {
        when(review.checking()).thenReturn(checkingPullRequestsView());
        doAnswer(mergedAtOnce(noMergedPullRequestsView())).when(review).subscribe(any(), any(), any());

        assertThat(linesWritten(review, request(1, "review/watch", reviewParams())))
                .containsExactly(ready(), resultOf(1, checking()), mergedChanged("[]"));
    }

    @Test
    void opens_the_watch_with_the_project_it_is_asked_for() {
        when(review.checking()).thenReturn(checkingPullRequestsView());

        linesWritten(review, request(1, "review/watch", reviewParams()));

        verify(review).subscribe(eq(new ReviewPayload(projectPath())), any(), any());
    }

    @Test
    void opens_no_second_watch_of_the_same_project() {
        when(review.checking()).thenReturn(checkingPullRequestsView());

        linesWritten(review, request(1, "review/watch", reviewParams()), request(2, "review/watch", reviewParams()));

        verify(review, times(1)).subscribe(any(), any(), any());
    }

    @Test
    void opens_a_watch_of_another_project() {
        when(review.checking()).thenReturn(checkingPullRequestsView());

        linesWritten(review, request(1, "review/watch", reviewParams()), request(2, "review/watch", otherReviewParams()));

        verify(review, times(2)).subscribe(any(), any(), any());
    }

    @Test
    void opens_the_watch_again_on_a_request_that_overlaps_one_failing_to_start() {
        CountDownLatch failing = new CountDownLatch(1);
        when(review.checking()).thenReturn(checkingPullRequestsView()).thenAnswer(waitingFor(failing, checkingPullRequestsView()));
        doAnswer(failingAfterReleasing(failing)).doNothing().when(review).subscribe(any(), any(), any());

        linesWritten(review, request(1, "review/watch", reviewParams()), request(2, "review/watch", reviewParams()));

        verify(review, times(2).description("a watch that let the second request in before forgetting the failed claim leaves the pull requests unwatched"))
                .subscribe(any(), any(), any());
    }
}

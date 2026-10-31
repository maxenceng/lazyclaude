package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.addedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManagerFixture.diffTooLarge;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.threadAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.onLineView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.onNoLineView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.pullRequestPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.tooLargeDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.viewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQueryFixture.reviewQuery;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffPort;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.domain.ThreadPort;

@ExtendWith(MockitoExtension.class)
class ReviewQueryPullRequestTest {

    private static final PullRequestPayload OF_THE_PULL_REQUEST = pullRequestPayload(project().root(), pullRequestNumber());

    @Mock
    private PullRequestDiffPort diffs;

    @Mock
    private ThreadPort threads;

    private ReviewQuery query;

    @BeforeEach
    void createQuery() {
        query = reviewQuery(diffs, threads);
    }

    @Test
    void shouldViewTheDiffAsLoadingBeforeItsReadAnswers() {
        assertThat(query.loadingDiff()).isEqualTo(loadingDiffView());
    }

    @Test
    void shouldViewTheThreadsAsLoadingBeforeTheirReadAnswers() {
        assertThat(query.loadingThreads()).isEqualTo(loadingThreadsView());
    }

    @Test
    void shouldViewTheFilesOfTheDiffInTheOrderItListsThem() {
        when(diffs.diff(project(), pullRequestNumber())).thenReturn(pullRequestDiffOf(modifiedFile(), addedFile()));

        PullRequestDiffView view = query.diffOf(OF_THE_PULL_REQUEST);

        assertThat(view).isInstanceOf(ReadDiffView.class);
        assertThat(((ReadDiffView) view).files()).extracting(DiffFileView::path).containsExactly(diffPath(), secondDiffPath());
    }

    @Test
    void shouldViewWhyTheDiffCannotBeReadInsteadOfFailing() {
        when(diffs.diff(project(), pullRequestNumber())).thenThrow(pullRequestsUnavailable());

        assertThat(query.diffOf(OF_THE_PULL_REQUEST)).isEqualTo(unavailableDiffView());
    }

    @Test
    void shouldViewADiffGitHubRefusesAsTooLarge() {
        when(diffs.diff(project(), pullRequestNumber())).thenThrow(diffTooLarge());

        assertThat(query.diffOf(OF_THE_PULL_REQUEST)).isEqualTo(tooLargeDiffView());
    }

    @Test
    void shouldViewTheThreadsByFileAndLineWithWhetherEachIsOpen() {
        ReviewThread late = threadAt(secondDiffPath(), rightOn(9));
        ReviewThread early = threadAt(diffPath(), rightOn(2));
        when(threads.threads(project(), pullRequestNumber())).thenReturn(List.of(late, early));

        assertThat(query.threadsOf(OF_THE_PULL_REQUEST)).isEqualTo(readThreadsViewOf(viewOf(early), viewOf(late)));
    }

    @Test
    void shouldViewWhyTheThreadsCannotBeReadInsteadOfFailing() {
        when(threads.threads(project(), pullRequestNumber())).thenThrow(pullRequestsUnavailable());

        assertThat(query.threadsOf(OF_THE_PULL_REQUEST)).isEqualTo(unavailableThreadsView());
    }

    @Test
    void shouldPlaceTheThreadsItHandedOutOnTheDiffItHandedOut() {
        ReviewThreadView onTheOldSide = openThreadView(diffPath(), leftOn(3));
        ReviewThreadView outdated = outdatedThreadView(diffPath());

        assertThat(query.threadsInDiff(new ThreadsInDiffPayload(twoFileDiffView(), readThreadsViewOf(outdated, onTheOldSide))))
                .isEqualTo(new ThreadsInDiffView(List.of(onLineView(onTheOldSide, 1), onNoLineView(outdated))));
    }

    @Test
    void shouldPutEveryThreadOnNoLineWhileTheDiffItHandedOutIsNotRead() {
        ReviewThreadView thread = openThreadView(diffPath(), rightOn(3));

        assertThat(query.threadsInDiff(new ThreadsInDiffPayload(tooLargeDiffView(), readThreadsViewOf(thread))))
                .isEqualTo(new ThreadsInDiffView(List.of(onNoLineView(thread))));
        assertThat(query.threadsInDiff(new ThreadsInDiffPayload(loadingDiffView(), readThreadsViewOf(thread))))
                .isEqualTo(new ThreadsInDiffView(List.of(onNoLineView(thread))));
    }

    @Test
    void shouldPlaceNoThreadWhileTheThreadsItHandedOutAreNotRead() {
        assertThat(query.threadsInDiff(new ThreadsInDiffPayload(twoFileDiffView(), loadingThreadsView()))).isEqualTo(new ThreadsInDiffView(List.of()));
        assertThat(query.threadsInDiff(new ThreadsInDiffPayload(twoFileDiffView(), unavailableThreadsView()))).isEqualTo(new ThreadsInDiffView(List.of()));
    }
}

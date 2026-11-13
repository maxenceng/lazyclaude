package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.addedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManagerFixture.diffTooLarge;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.threadAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.loadingPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.atFileHeadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.onLineView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outsideDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.pullRequestPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.resolvedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.threadsOnlyPayload;
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

        assertThat(query.commentsInDiff(threadsOnlyPayload(twoFileDiffView(), readThreadsViewOf(outdated, onTheOldSide))).threads())
                .isEqualTo(List.of(onLineView(onTheOldSide, 1), atFileHeadView(outdated, 0)));
    }

    @Test
    void shouldPutEveryThreadOutsideTheDiffWhileTheDiffItHandedOutIsNotRead() {
        ReviewThreadView thread = openThreadView(diffPath(), rightOn(3));

        assertThat(query.commentsInDiff(threadsOnlyPayload(tooLargeDiffView(), readThreadsViewOf(thread))).threads()).isEqualTo(List.of(outsideDiffView(thread)));
        assertThat(query.commentsInDiff(threadsOnlyPayload(loadingDiffView(), readThreadsViewOf(thread))).threads()).isEqualTo(List.of(outsideDiffView(thread)));
    }

    @Test
    void shouldPlaceNoThreadWhileTheThreadsItHandedOutAreNotRead() {
        assertThat(query.commentsInDiff(threadsOnlyPayload(twoFileDiffView(), loadingThreadsView())).threads()).isEqualTo(List.of());
        assertThat(query.commentsInDiff(threadsOnlyPayload(twoFileDiffView(), unavailableThreadsView())).threads()).isEqualTo(List.of());
    }

    @Test
    void shouldKeepAResolvedThreadFromUnderItsLineUntilResolvedThreadsAreShown() {
        ReviewThreadView open = openThreadView(diffPath(), leftOn(3));
        ReviewThreadView resolved = resolvedThreadView(diffPath(), rightOn(3));
        ReadThreadsView threads = readThreadsViewOf(open, resolved);

        assertThat(query.commentsInDiff(new CommentsInDiffPayload(twoFileDiffView(), threads, loadingPendingView(), false)).shown())
                .as("the TUI's r and the window's r both ask review which threads show")
                .isEqualTo(List.of(onLineView(open, 1)));
        assertThat(query.commentsInDiff(new CommentsInDiffPayload(twoFileDiffView(), threads, loadingPendingView(), true)).shown())
                .isEqualTo(List.of(onLineView(open, 1), onLineView(resolved, 2)));
    }

    @Test
    void shouldListEveryThreadInCommentsResolvedOnesIncludedWhetherOrNotTheyShowUnderTheirLines() {
        ReviewThreadView resolved = resolvedThreadView(diffPath(), rightOn(3));

        assertThat(query.commentsInDiff(new CommentsInDiffPayload(twoFileDiffView(), readThreadsViewOf(resolved), loadingPendingView(), false)).listed())
                .isEqualTo(List.of(onLineView(resolved, 2)));
    }
}

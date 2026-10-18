package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManagerFixture.diffTooLarge;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManagerFixture.tooLargeDiff;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManagerFixture.unavailableDiff;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManagerFixture.unavailableThreads;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.onLine;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.onNoLine;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.outdatedThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.reviewThread;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PullRequestDiffManagerTest {

    @Mock
    private PullRequestDiffPort diffs;

    @Mock
    private ThreadPort threads;

    private PullRequestDiffManager manager;

    @BeforeEach
    void createManager() {
        manager = new PullRequestDiffManager(diffs, threads);
    }

    @Test
    void shouldBeLoadingTheDiffBeforeTheReadAnswers() {
        assertThat(manager.loadingDiff()).isEqualTo(new LoadingDiff());
    }

    @Test
    void shouldBeLoadingTheThreadsBeforeTheReadAnswers() {
        assertThat(manager.loadingThreads()).isEqualTo(new LoadingThreads());
    }

    @Test
    void shouldReadTheDiffOfThePullRequest() {
        when(diffs.diff(project(), pullRequestNumber())).thenReturn(pullRequestDiffOf(modifiedFile()));

        assertThat(manager.diff(project(), pullRequestNumber())).isEqualTo(new ReadDiff(pullRequestDiffOf(modifiedFile())));
    }

    @Test
    void shouldAnswerWhyTheDiffCannotBeReadInsteadOfFailing() {
        when(diffs.diff(project(), pullRequestNumber())).thenThrow(pullRequestsUnavailable());

        assertThat(manager.diff(project(), pullRequestNumber())).isEqualTo(unavailableDiff());
    }

    @Test
    void shouldAnswerThatTheDiffIsTooLargeWhenGitHubRefusesIt() {
        when(diffs.diff(project(), pullRequestNumber())).thenThrow(diffTooLarge());

        assertThat(manager.diff(project(), pullRequestNumber())).isEqualTo(tooLargeDiff());
    }

    @Test
    void shouldReadTheThreadsOfThePullRequest() {
        when(threads.threads(project(), pullRequestNumber())).thenReturn(List.of(reviewThread()));

        assertThat(manager.threads(project(), pullRequestNumber())).isEqualTo(new ReadThreads(List.of(reviewThread())));
    }

    @Test
    void shouldAnswerWhyTheThreadsCannotBeReadInsteadOfFailing() {
        when(threads.threads(project(), pullRequestNumber())).thenThrow(pullRequestsUnavailable());

        assertThat(manager.threads(project(), pullRequestNumber())).isEqualTo(unavailableThreads());
    }

    @Test
    void shouldPlaceTheThreadsOnTheDiffOnceBothAreRead() {
        assertThat(manager.threadsInDiff(new ReadDiff(pullRequestDiffOf(modifiedFile())), new ReadThreads(List.of(reviewThread()))))
                .isEqualTo(new ThreadsInDiff(List.of(onLine(reviewThread(), 2))));
    }

    @Test
    void shouldPutEveryThreadOnNoLineUntilTheDiffIsRead() {
        assertThat(manager.threadsInDiff(unavailableDiff(), new ReadThreads(List.of(outdatedThread(), reviewThread()))))
                .as("the Comments panel lists the threads as soon as they are read, whatever became of the diff")
                .isEqualTo(new ThreadsInDiff(List.of(onNoLine(reviewThread()), onNoLine(outdatedThread()))));
    }

    @Test
    void shouldHoldNoThreadUntilTheThreadsAreRead() {
        assertThat(manager.threadsInDiff(new ReadDiff(pullRequestDiffOf(modifiedFile())), unavailableThreads()))
                .isEqualTo(new ThreadsInDiff(List.of()));
    }
}

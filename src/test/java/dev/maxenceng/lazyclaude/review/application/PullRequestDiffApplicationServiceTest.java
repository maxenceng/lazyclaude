package dev.maxenceng.lazyclaude.review.application;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
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

import dev.maxenceng.lazyclaude.review.domain.LoadingDiff;
import dev.maxenceng.lazyclaude.review.domain.LoadingThreads;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffPort;
import dev.maxenceng.lazyclaude.review.domain.ReadDiff;
import dev.maxenceng.lazyclaude.review.domain.ReadThreads;
import dev.maxenceng.lazyclaude.review.domain.ThreadPort;

@ExtendWith(MockitoExtension.class)
class PullRequestDiffApplicationServiceTest {

    @Mock
    private PullRequestDiffPort diffs;

    @Mock
    private ThreadPort threads;

    private PullRequestDiffApplicationService service;

    @BeforeEach
    void createService() {
        service = new PullRequestDiffApplicationService(diffs, threads);
    }

    @Test
    void shouldBeLoadingBeforeTheReadsAnswer() {
        assertThat(service.loadingDiff()).isEqualTo(new LoadingDiff());
        assertThat(service.loadingThreads()).isEqualTo(new LoadingThreads());
    }

    @Test
    void shouldReadTheDiffOfThePullRequest() {
        when(diffs.diff(project(), pullRequestNumber())).thenReturn(pullRequestDiffOf(modifiedFile()));

        assertThat(service.diff(project(), pullRequestNumber())).isEqualTo(new ReadDiff(pullRequestDiffOf(modifiedFile())));
    }

    @Test
    void shouldReadTheThreadsOfThePullRequest() {
        when(threads.threads(project(), pullRequestNumber())).thenReturn(List.of(reviewThread()));

        assertThat(service.threads(project(), pullRequestNumber())).isEqualTo(new ReadThreads(List.of(reviewThread())));
    }
}

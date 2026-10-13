package dev.maxenceng.lazyclaude.review.application;

import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.openPullRequests;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.watchedRefresh;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.review.domain.CheckingPullRequests;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequests;

@ExtendWith(MockitoExtension.class)
class ReviewApplicationServiceTest {

    @Mock
    private OpenPullRequestPort pullRequests;

    @Mock
    private PullRequestWatchPort watch;

    @Test
    void shouldBeCheckingBeforeTheFirstFetchAnswers() {
        assertThat(new ReviewApplicationService(pullRequests, watch).checking()).isEqualTo(new CheckingPullRequests());
    }

    @Test
    void shouldHandTheListenerTheOpenPullRequestsOnEachFetch() {
        when(pullRequests.openPullRequests(project())).thenReturn(openPullRequests().pullRequests());
        List<PullRequests> delivered = new ArrayList<>();
        new ReviewApplicationService(pullRequests, watch).subscribe(project(), delivered::add);

        watchedRefresh(watch).run();

        assertThat(delivered).containsExactly(openPullRequests());
    }
}

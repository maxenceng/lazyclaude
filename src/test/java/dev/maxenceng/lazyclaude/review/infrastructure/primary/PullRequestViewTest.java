package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.failedPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.passedPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pendingPullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

class PullRequestViewTest {

    @Test
    void shouldHaveTheOutcomeOfTheWorstCheckAsItsCiResult() {
        assertThat(failedPullRequestView().ciResult()).contains(CheckOutcome.FAILED);
        assertThat(pendingPullRequestView().ciResult()).contains(CheckOutcome.PENDING);
        assertThat(passedPullRequestView().ciResult()).contains(CheckOutcome.PASSED);
    }

    @Test
    void shouldHaveNoCiResultWithoutChecks() {
        assertThat(pullRequestView().ciResult())
                .as("passed would claim a green run that never happened")
                .isEmpty();
    }
}

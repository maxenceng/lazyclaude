package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.siblingPullRequestView;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.bareId;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.headBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.sharingTheIdPrefix;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenPullRequestsViewTest {

    @Test
    void shouldFindThePullRequestWhoseHeadIsExactlyTheBranch() {
        assertThat(openPullRequestsViewOf(siblingPullRequestView(), pullRequestView()).on(headBranch()))
                .contains(pullRequestView());
    }

    @Test
    void shouldFindNothingForAHeadThatOnlySharesTheIdPrefixOrIsTheBareId() {
        assertThat(openPullRequestsViewOf(pullRequestView()).on(sharingTheIdPrefix())).isEmpty();
        assertThat(openPullRequestsViewOf(pullRequestView()).on(bareId())).isEmpty();
    }
}

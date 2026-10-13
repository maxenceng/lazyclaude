package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.bareIdPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.longerHeadPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.pullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.sharingTheIdPrefixPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.siblingPullRequest;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.headBranch;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OpenPullRequestsTest {

    @Test
    void shouldFindThePullRequestWhoseHeadIsExactlyTheBranch() {
        assertThat(new OpenPullRequests(List.of(siblingPullRequest(), pullRequest())).on(headBranch()))
                .contains(pullRequest());
    }

    @Test
    void shouldNotFindAPullRequestWhoseHeadOnlySharesTheIdPrefix() {
        assertThat(new OpenPullRequests(List.of(sharingTheIdPrefixPullRequest())).on(headBranch()))
                .as("PROJ-10-… is another ticket's branch, not PROJ-1's")
                .isEmpty();
    }

    @Test
    void shouldNotFindAPullRequestWhoseHeadMerelyStartsWithTheBranch() {
        assertThat(new OpenPullRequests(List.of(longerHeadPullRequest())).on(headBranch())).isEmpty();
    }

    @Test
    void shouldNotFindAPullRequestWhoseHeadIsTheBareId() {
        assertThat(new OpenPullRequests(List.of(bareIdPullRequest())).on(headBranch())).isEmpty();
    }

    @Test
    void shouldFindNothingWhenNoPullRequestIsOpen() {
        assertThat(new OpenPullRequests(List.of()).on(headBranch())).isEmpty();
    }
}

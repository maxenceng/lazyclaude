package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.checksOf;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.failedBuild;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.noChecks;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.passedLint;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.pendingContract;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.bareId;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.headBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.longerThanTheBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.sharingTheIdPrefix;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.siblingHeadBranch;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestTitle;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.secondPullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.secondPullRequestTitle;

import dev.maxenceng.lazyclaude.review.domain.PullRequest.PullRequestBuilder;

public final class PullRequestFixture {

    private PullRequestFixture() {}

    public static PullRequestBuilder pullRequestBuilder() {
        return PullRequest.builder()
                .number(pullRequestNumber())
                .title(pullRequestTitle())
                .head(headBranch())
                .draft(false)
                .checks(noChecks());
    }

    public static PullRequest pullRequest() {
        return pullRequestBuilder().build();
    }

    public static PullRequest draftPullRequest() {
        return pullRequestBuilder().draft(true).build();
    }

    public static PullRequest failingPullRequest() {
        return pullRequestBuilder().checks(checksOf(passedLint(), pendingContract(), failedBuild())).build();
    }

    public static PullRequest pendingPullRequest() {
        return pullRequestBuilder().checks(checksOf(pendingContract(), passedLint())).build();
    }

    public static PullRequest siblingPullRequest() {
        return pullRequestBuilder()
                .number(secondPullRequestNumber())
                .title(secondPullRequestTitle())
                .head(siblingHeadBranch())
                .build();
    }

    public static PullRequest sharingTheIdPrefixPullRequest() {
        return pullRequestBuilder().head(sharingTheIdPrefix()).build();
    }

    public static PullRequest bareIdPullRequest() {
        return pullRequestBuilder().head(bareId()).build();
    }

    public static PullRequest longerHeadPullRequest() {
        return pullRequestBuilder().head(longerThanTheBranch()).build();
    }
}

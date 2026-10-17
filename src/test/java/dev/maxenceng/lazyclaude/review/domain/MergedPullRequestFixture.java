package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.headBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.siblingHeadBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.secondHeadCommit;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.secondPullRequestNumber;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest.MergedPullRequestBuilder;

public final class MergedPullRequestFixture {

    private MergedPullRequestFixture() {}

    public static MergedPullRequestBuilder mergedPullRequestBuilder() {
        return MergedPullRequest.builder()
                .number(pullRequestNumber())
                .head(headBranch())
                .headCommit(headCommit());
    }

    public static MergedPullRequest mergedPullRequest() {
        return mergedPullRequestBuilder().build();
    }

    public static MergedPullRequest siblingMergedPullRequest() {
        return mergedPullRequestBuilder()
                .number(secondPullRequestNumber())
                .head(siblingHeadBranch())
                .headCommit(secondHeadCommit())
                .build();
    }
}

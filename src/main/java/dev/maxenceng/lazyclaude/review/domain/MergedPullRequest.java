package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.HeadCommit;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;

/** A pull request on this repository, not from a fork, that GitHub reports merged: its head branch, and the commit that head was at. */
public record MergedPullRequest(PullRequestNumber number, HeadBranch head, HeadCommit headCommit) {

    public MergedPullRequest {
        Assert.notNull("pull request number", number);
        Assert.notNull("head branch", head);
        Assert.notNull("head commit", headCommit);
    }

    public static MergedPullRequestBuilder builder() {
        return new MergedPullRequestBuilder();
    }

    public static final class MergedPullRequestBuilder {
        private PullRequestNumber number;
        private HeadBranch head;
        private HeadCommit headCommit;

        private MergedPullRequestBuilder() {}

        public MergedPullRequestBuilder number(PullRequestNumber number) {
            this.number = number;
            return this;
        }

        public MergedPullRequestBuilder head(HeadBranch head) {
            this.head = head;
            return this;
        }

        public MergedPullRequestBuilder headCommit(HeadCommit headCommit) {
            this.headCommit = headCommit;
            return this;
        }

        public MergedPullRequest build() {
            return new MergedPullRequest(number, head, headCommit);
        }
    }
}

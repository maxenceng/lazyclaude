package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.CommitSha;

/** A pull request as a review of it sees it: GitHub's id of it, the commit its head is at, and your review of it, if any. */
public record ReviewedPullRequest(PullRequestNodeId id, CommitSha head, ReviewInProgress review) {

    public ReviewedPullRequest {
        Assert.notNull("id", id);
        Assert.notNull("head", head);
        Assert.notNull("review", review);
    }

    public static ReviewedPullRequestBuilder builder() {
        return new ReviewedPullRequestBuilder();
    }

    public static final class ReviewedPullRequestBuilder {
        private PullRequestNodeId id;
        private CommitSha head;
        private ReviewInProgress review;

        private ReviewedPullRequestBuilder() {}

        public ReviewedPullRequestBuilder id(PullRequestNodeId id) {
            this.id = id;
            return this;
        }

        public ReviewedPullRequestBuilder head(CommitSha head) {
            this.head = head;
            return this;
        }

        public ReviewedPullRequestBuilder review(ReviewInProgress review) {
            this.review = review;
            return this;
        }

        public ReviewedPullRequest build() {
            return new ReviewedPullRequest(id, head, review);
        }
    }
}

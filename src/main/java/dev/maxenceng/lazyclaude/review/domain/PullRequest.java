package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;

/**
 * An open pull request: where it comes from, whether GitHub marks it draft, when it was opened, the checks on its head, its
 * comments others can see, whether you hold an unsubmitted review on it, and the issue it closes.
 */
public record PullRequest(
        PullRequestNumber number,
        PullRequestTitle title,
        HeadBranch head,
        boolean draft,
        OpenedAt openedAt,
        Checks checks,
        CommentCount comments,
        boolean pendingReview,
        IssueClosing closing) {

    public PullRequest {
        Assert.notNull("pull request number", number);
        Assert.notNull("pull request title", title);
        Assert.notNull("head branch", head);
        Assert.notNull("opened at", openedAt);
        Assert.notNull("checks", checks);
        Assert.notNull("comments", comments);
        Assert.notNull("closing", closing);
    }

    public static PullRequestBuilder builder() {
        return new PullRequestBuilder();
    }

    public static final class PullRequestBuilder {
        private PullRequestNumber number;
        private PullRequestTitle title;
        private HeadBranch head;
        private boolean draft;
        private OpenedAt openedAt;
        private Checks checks;
        private CommentCount comments;
        private boolean pendingReview;
        private IssueClosing closing;

        private PullRequestBuilder() {}

        public PullRequestBuilder number(PullRequestNumber number) {
            this.number = number;
            return this;
        }

        public PullRequestBuilder title(PullRequestTitle title) {
            this.title = title;
            return this;
        }

        public PullRequestBuilder head(HeadBranch head) {
            this.head = head;
            return this;
        }

        public PullRequestBuilder draft(boolean draft) {
            this.draft = draft;
            return this;
        }

        public PullRequestBuilder openedAt(OpenedAt openedAt) {
            this.openedAt = openedAt;
            return this;
        }

        public PullRequestBuilder checks(Checks checks) {
            this.checks = checks;
            return this;
        }

        public PullRequestBuilder comments(CommentCount comments) {
            this.comments = comments;
            return this;
        }

        public PullRequestBuilder pendingReview(boolean pendingReview) {
            this.pendingReview = pendingReview;
            return this;
        }

        public PullRequestBuilder closing(IssueClosing closing) {
            this.closing = closing;
            return this;
        }

        public PullRequest build() {
            return new PullRequest(number, title, head, draft, openedAt, checks, comments, pendingReview, closing);
        }
    }
}

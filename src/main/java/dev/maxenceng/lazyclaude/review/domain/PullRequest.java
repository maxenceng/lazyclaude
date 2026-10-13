package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;

/** An open pull request: where it comes from, whether GitHub marks it draft, and the checks on its head. */
public record PullRequest(
        PullRequestNumber number, PullRequestTitle title, HeadBranch head, boolean draft, Checks checks) {

    public PullRequest {
        Assert.notNull("pull request number", number);
        Assert.notNull("pull request title", title);
        Assert.notNull("head branch", head);
        Assert.notNull("checks", checks);
    }

    public static PullRequestBuilder builder() {
        return new PullRequestBuilder();
    }

    public static final class PullRequestBuilder {
        private PullRequestNumber number;
        private PullRequestTitle title;
        private HeadBranch head;
        private boolean draft;
        private Checks checks;

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

        public PullRequestBuilder checks(Checks checks) {
            this.checks = checks;
            return this;
        }

        public PullRequest build() {
            return new PullRequest(number, title, head, draft, checks);
        }
    }
}

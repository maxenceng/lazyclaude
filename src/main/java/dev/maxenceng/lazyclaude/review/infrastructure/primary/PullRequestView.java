package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.review.domain.CommentCount;
import dev.maxenceng.lazyclaude.review.domain.IssueClosing;
import dev.maxenceng.lazyclaude.review.domain.OpenedAt;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;

/**
 * One open pull request as the shell reads it: when it was opened, its CI result, and its checks failed first, then pending, then
 * passed, how many comments others can see, whether you hold an unsubmitted review on it, and the issue it closes.
 */
public record PullRequestView(
        PullRequestNumber number,
        PullRequestTitle title,
        HeadBranch head,
        boolean draft,
        OpenedAt openedAt,
        List<CheckView> checks,
        CommentCount comments,
        boolean pendingReview,
        IssueClosing closing) {

    /** The outcome of the worst check, since they are listed worst first; none for a pull request with no checks. */
    public Optional<CheckOutcome> ciResult() {
        return checks.stream().findFirst().map(CheckView::outcome);
    }

    static PullRequestView from(PullRequest pullRequest) {
        return builder()
                .number(pullRequest.number())
                .title(pullRequest.title())
                .head(pullRequest.head())
                .draft(pullRequest.draft())
                .openedAt(pullRequest.openedAt())
                .checks(pullRequest.checks().listed().stream().map(CheckView::from).toList())
                .comments(pullRequest.comments())
                .pendingReview(pullRequest.pendingReview())
                .closing(pullRequest.closing())
                .build();
    }

    public static PullRequestViewBuilder builder() {
        return new PullRequestViewBuilder();
    }

    public static final class PullRequestViewBuilder {
        private PullRequestNumber number;
        private PullRequestTitle title;
        private HeadBranch head;
        private boolean draft;
        private OpenedAt openedAt;
        private List<CheckView> checks;
        private CommentCount comments;
        private boolean pendingReview;
        private IssueClosing closing;

        private PullRequestViewBuilder() {}

        public PullRequestViewBuilder number(PullRequestNumber number) {
            this.number = number;
            return this;
        }

        public PullRequestViewBuilder title(PullRequestTitle title) {
            this.title = title;
            return this;
        }

        public PullRequestViewBuilder head(HeadBranch head) {
            this.head = head;
            return this;
        }

        public PullRequestViewBuilder draft(boolean draft) {
            this.draft = draft;
            return this;
        }

        public PullRequestViewBuilder openedAt(OpenedAt openedAt) {
            this.openedAt = openedAt;
            return this;
        }

        public PullRequestViewBuilder checks(List<CheckView> checks) {
            this.checks = checks;
            return this;
        }

        public PullRequestViewBuilder comments(CommentCount comments) {
            this.comments = comments;
            return this;
        }

        public PullRequestViewBuilder pendingReview(boolean pendingReview) {
            this.pendingReview = pendingReview;
            return this;
        }

        public PullRequestViewBuilder closing(IssueClosing closing) {
            this.closing = closing;
            return this;
        }

        public PullRequestView build() {
            return new PullRequestView(number, title, head, draft, openedAt, checks, comments, pendingReview, closing);
        }
    }
}

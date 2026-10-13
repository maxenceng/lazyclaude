package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;

/** One open pull request as the shell reads it: its CI result, and its checks failed first, then pending, then passed. */
public record PullRequestView(
        PullRequestNumber number,
        PullRequestTitle title,
        HeadBranch head,
        boolean draft,
        List<CheckView> checks) {

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
                .checks(pullRequest.checks().listed().stream().map(CheckView::from).toList())
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
        private List<CheckView> checks;

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

        public PullRequestViewBuilder checks(List<CheckView> checks) {
            this.checks = checks;
            return this;
        }

        public PullRequestView build() {
            return new PullRequestView(number, title, head, draft, checks);
        }
    }
}

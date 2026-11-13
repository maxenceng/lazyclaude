package dev.maxenceng.lazyclaude.review.domain;

import java.time.Instant;

public final class PullRequestDomainFieldsFixture {

    private PullRequestDomainFieldsFixture() {}

    public static OpenedAt openedAt() {
        return new OpenedAt(Instant.parse("2026-11-13T10:21:46Z"));
    }

    public static CommentCount noComments() {
        return new CommentCount(0);
    }

    public static CommentCount twoComments() {
        return new CommentCount(2);
    }

    public static IssueClosing closesNoIssue() {
        return new ClosesNoIssue();
    }

    public static IssueClosing closesIssue() {
        return new ClosesIssue(closingIssueNumber());
    }

    public static ClosingIssueNumber closingIssueNumber() {
        return new ClosingIssueNumber(215);
    }
}
